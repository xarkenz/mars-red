package mars.assembler;

import mars.ErrorList;
import mars.ErrorMessage;

import java.nio.file.Path;
import java.util.*;

// TODO: document
public class Preprocessor {
    private final Path mainFilePath;
    private final Set<Path> knownFilePaths;
    private final Map<String, List<Token>> equivalences;

    public Preprocessor(String filename) {
        this.mainFilePath = Path.of(filename).toAbsolutePath();
        this.knownFilePaths = new HashSet<>();
        this.equivalences = new HashMap<>();
    }

    public boolean checkFilename(String filename) {
        return !this.knownFilePaths.add(this.mainFilePath.resolveSibling(Path.of(filename)));
    }

    public void processLine(List<SourceLine> destination, SourceLine line, ErrorList errors) {
        List<Token> tokens = line.getTokens();
        // Check for a preprocessor directive, which may not necessarily be the first token (e.g. follows a label)
        for (int index = 0; index < tokens.size(); index++) {
            Token token = tokens.get(index);
            // Check for .include
            if (token.getValue() == Directive.INCLUDE) {
                // Token after .include must be a string filename
                if (index + 1 >= tokens.size() || tokens.get(index + 1).getType() != TokenType.STRING) {
                    errors.add(new ErrorMessage(
                        token.getSourceFilename(),
                        token.getSourceLine(),
                        token.getSourceColumn(),
                        "Expected a filename string following '" + Directive.INCLUDE + "'"
                    ));
                    return;
                }
                String filename = (String) tokens.get(index + 1).getValue();
                // Convert filename to an absolute path based on the path of the main file
                filename = this.mainFilePath.resolveSibling(Path.of(filename)).toString();
                // If the file fails to open, includedLines will just be empty
                List<SourceLine> includedLines = Tokenizer.tokenize(filename, errors, this);
                // Remove the .include directive from the original line, but still keep the rest of the line
                // in case it has a label or something before the directive
                tokens.subList(index, index + 2).clear();
                // FIXME: what to do about the rest of the tokens in the line?
                // Add the modified line, then paste in the included file's lines
                destination.add(line);
                destination.addAll(includedLines);
                return;
            }
            // Check for .eqv
            else if (token.getValue() == Directive.EQV) {
                // Token after .eqv must be an identifier
                if (index + 1 >= tokens.size() || tokens.get(index + 1).getType() != TokenType.IDENTIFIER) {
                    errors.add(new ErrorMessage(
                        token.getSourceFilename(),
                        token.getSourceLine(),
                        token.getSourceColumn(),
                        "Expected an identifier following '" + Directive.EQV + "'"
                    ));
                    return;
                }
                token = tokens.get(index + 1);
                String equivalenceKey = token.getLiteral();
                // Equivalences cannot be redefined-- the only reason for this is to act like the GNU .eqv
                if (this.equivalences.containsKey(equivalenceKey)) {
                    errors.add(new ErrorMessage(
                        token.getSourceFilename(),
                        token.getSourceLine(),
                        token.getSourceColumn(),
                        "The equivalence '" + equivalenceKey + "' has already been defined"
                    ));
                    // Even though this is an error, we can still process it as if it's valid, so continue
                }
                List<Token> equivalentTokens = new ArrayList<>(tokens.subList(index + 2, tokens.size()));
                // Remove any comments so they don't interfere when the equivalence is used later
                equivalentTokens.removeIf(equivalentToken -> equivalentToken.getType() == TokenType.COMMENT);
                this.equivalences.put(equivalenceKey, equivalentTokens);
                // Remove the .eqv directive from the original line, but still keep the rest of the line
                // in case it has a label or something before the directive
                tokens.subList(index, tokens.size()).clear();
                // Add the modified line
                destination.add(line);
                return;
            }
        }
        // No preprocessor directive found, so add the line without any processing
        destination.add(line);
    }

    public void processToken(List<Token> destination, Token token) {
        if (token.getType() == TokenType.IDENTIFIER) {
            // Check if an equivalence has been defined for this identifier
            List<Token> equivalentTokens = this.equivalences.get(token.getLiteral());
            if (equivalentTokens != null) {
                // There is an equivalence defined, so paste in the substitution
                for (Token equivalentToken : equivalentTokens) {
                    Token substituteToken = new Token(
                        // The token content is copied from the definition...
                        equivalentToken.getType(),
                        equivalentToken.getValue(),
                        equivalentToken.getLiteral(),
                        // ...but we'll just say it's located where the identifier was
                        token.getSourceFilename(),
                        token.getSourceLine(),
                        token.getSourceColumn()
                    );
                    substituteToken.setOriginalToken(equivalentToken);
                    destination.add(substituteToken);
                }
                return;
            }
            // DPS 03-Jan-2013. Related to 11-July-2012.
            // This statement will replace original source with source modified by .eqv substitution.
            // Not needed by assembler, but looks better in the Text Segment Display.
            // TODO: restore the behavior mentioned in this comment?
        }
        // Add the token to the list as usual
        destination.add(token);
    }
}
