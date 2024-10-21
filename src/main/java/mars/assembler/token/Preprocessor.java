package mars.assembler.token;

import mars.ErrorList;
import mars.ErrorMessage;
import mars.assembler.Directive;
import mars.assembler.SourceFile;
import mars.assembler.SourceLine;

import java.nio.file.Path;
import java.util.*;

/**
 * @author Sean Clarke 08/2024
 */
public class Preprocessor {
    private record DirectiveLocation(Path currentPath, int lineIndex) {}

    private final Set<DirectiveLocation> knownIncludeDirectiveLocations;
    private final Map<String, List<Token>> equivalences;
    private final MacroHandler macroHandler;
    private Macro currentMacro;

    public Preprocessor() {
        this.knownIncludeDirectiveLocations = new HashSet<>();
        this.equivalences = new HashMap<>();
        this.macroHandler = new MacroHandler();
        this.currentMacro = null;
    }

    public void processLine(List<SourceLine> destination, SourceLine line, ErrorList errors) {
        List<Token> tokens = line.tokens();

        // If a macro is currently being built, the resulting line should go to the macro instead
        if (this.currentMacro != null) {
            destination = this.currentMacro.getLines();
        }

        // Check for a preprocessor directive, which may not necessarily be the first token (e.g. follows a label)
        for (int index = 0; index < tokens.size(); index++) {
            Token token = tokens.get(index);

            // Check for a macro call
            if (this.currentMacro == null && token.getType() == TokenType.IDENTIFIER) {
                // The macro name must be the first token in the line aside from a possible label
                if (index != 0 && tokens.get(index - 1).getType() != TokenType.COLON) {
                    // No need to check this token further-- equivalences have already been processed
                    continue;
                }

                String macroName = token.getLiteral();
                List<Token> arguments = this.macroHandler.getCallArguments(tokens.subList(index + 1, tokens.size()));

                // Obtain the macro which matches the list of arguments, if one exists
                Macro macro = this.macroHandler.findMatchingMacro(macroName, arguments);
                if (macro == null) {
                    // No need to check this token further
                    continue;
                }

                // Expand the macro with the current arguments
                List<SourceLine> instanceLines = this.macroHandler.instantiate(macro, arguments, line, errors);

                // Remove the macro call from the original line, but still keep the beginning of the line
                // in case it has a label or something before the call
                tokens.subList(index, tokens.size()).clear();

                // Add the modified line, then paste in the macro expansion
                destination.add(line);
                destination.addAll(instanceLines);
                return;
            }
            // Check for .include
            else if (token.getValue() == Directive.INCLUDE) {
                // Token after .include must be a string filename
                if (index + 1 >= tokens.size() || tokens.get(index + 1).getType() != TokenType.STRING) {
                    errors.add(new ErrorMessage(
                        token.getFilename(),
                        token.getLineIndex(),
                        token.getColumnIndex(),
                        "Directive '" + Directive.INCLUDE + "' expects a string filename"
                    ));
                    continue;
                }

                // Ensure that we have not encountered this particular directive before
                DirectiveLocation location = new DirectiveLocation(Path.of(token.getFilename()).toAbsolutePath(), token.getLineIndex());
                if (!this.knownIncludeDirectiveLocations.add(location)) {
                    errors.add(new ErrorMessage(
                        token.getFilename(),
                        token.getLineIndex(),
                        token.getColumnIndex(),
                        "Recursive include detected at this directive"
                    ));
                    continue;
                }

                String filename = (String) tokens.get(index + 1).getValue();
                // Interpret the filename as a path resolved against the path of the current file
                Path path = Path.of(token.getFilename()).resolveSibling(filename).toAbsolutePath();
                // If the file fails to open, includedFile will just be empty
                SourceFile includedFile = Tokenizer.tokenizeFile(path.toString(), errors, this);

                // If there are extraneous tokens following the directive, clear them and warn the user
                if (index + 2 < tokens.size()) {
                    errors.add(new ErrorMessage(
                        true,
                        token.getFilename(),
                        token.getLineIndex(),
                        tokens.get(index + 2).getColumnIndex(),
                        "Ignoring extra arguments to '" + Directive.INCLUDE + "'",
                        ""
                    ));
                }
                // Remove the .include directive from the original line, but still keep the beginning of the line
                // in case it has a label or something before the directive
                tokens.subList(index, tokens.size()).clear();

                // Add the modified line, then paste in the included file's lines
                destination.add(line);
                destination.addAll(includedFile.getLines());
                return;
            }
            // Check for .eqv
            else if (token.getValue() == Directive.EQV) {
                if (index + 1 >= tokens.size()) {
                    errors.add(new ErrorMessage(
                        token.getFilename(),
                        token.getLineIndex(),
                        token.getColumnIndex(),
                        "Directive '" + Directive.EQV + "' requires an identifier followed by replacement"
                    ));
                    continue;
                }
                // Token after .eqv must be an identifier
                token = tokens.get(index + 1);
                if (token.getType() != TokenType.IDENTIFIER) {
                    errors.add(new ErrorMessage(
                        token.getFilename(),
                        token.getLineIndex(),
                        token.getColumnIndex(),
                        "Directive '" + Directive.EQV + "' expected an identifier, got: " + token
                    ));
                    continue;
                }

                String equivalenceKey = token.getLiteral();
                // Equivalences cannot be redefined-- the only reason for this is to act like the GNU .eqv
                if (this.equivalences.containsKey(equivalenceKey)) {
                    errors.add(new ErrorMessage(
                        true,
                        token.getFilename(),
                        token.getLineIndex(),
                        token.getColumnIndex(),
                        "The equivalence '" + equivalenceKey + "' has already been defined",
                        ""
                    ));
                    // Even though this is a warning, we can still process it as if it's valid, so continue
                }

                List<Token> equivalentTokens = new ArrayList<>(tokens.subList(index + 2, tokens.size()));
                // Remove any comments so they don't interfere when the equivalence is used later
                equivalentTokens.removeIf(equivalentToken -> equivalentToken.getType() == TokenType.COMMENT);
                this.equivalences.put(equivalenceKey, equivalentTokens);

                // Remove the .eqv directive from the original line, but still keep the beginning of the line
                // in case it has a label or something before the directive
                tokens.subList(index, tokens.size()).clear();

                // Add the modified line
                destination.add(line);
                return;
            }
            else if (this.currentMacro != null) {
                // Check for a label definition
                if (token.getType() == TokenType.IDENTIFIER || token.getType() == TokenType.OPERATOR) {
                    if (index + 1 < tokens.size() && tokens.get(index + 1).getType() == TokenType.COLON && (
                        index == 0 || tokens.get(index - 1).getType() == TokenType.COLON
                    )) {
                        if (!this.currentMacro.addLabel(token.getLiteral())) {
                            errors.add(new ErrorMessage(
                                token.getFilename(),
                                token.getLineIndex(),
                                token.getColumnIndex(),
                                "Label '" + token + "' has already been used in this macro definition"
                            ));
                        }
                    }
                }
                // Check for .end_macro to end the current macro
                else if (token.getValue() == Directive.END_MACRO) {
                    // End the current macro definition
                    this.currentMacro = null;

                    // If there are extraneous tokens following the directive, warn the user
                    if (index + 1 < tokens.size()) {
                        errors.add(new ErrorMessage(
                            true,
                            token.getFilename(),
                            token.getLineIndex(),
                            tokens.get(index + 1).getColumnIndex(),
                            "Ignoring extra content following '" + Directive.END_MACRO + "'",
                            ""
                        ));
                    }
                    // Remove the .end_macro directive from the original line, but still keep the beginning of the line
                    // in case it has a label or something before the directive
                    tokens.subList(index, tokens.size()).clear();

                    // Add the modified line
                    destination.add(line);
                    return;
                }
                // Check for .macro to prevent nested macro definitions
                else if (token.getValue() == Directive.MACRO) {
                    errors.add(new ErrorMessage(
                        token.getFilename(),
                        token.getLineIndex(),
                        token.getColumnIndex(),
                        "Nested macro definitions are not permitted"
                    ));
                }
            }
            // Check for .macro to start a new macro
            else if (token.getValue() == Directive.MACRO) {
                if (index + 1 >= tokens.size()) {
                    errors.add(new ErrorMessage(
                        token.getFilename(),
                        token.getLineIndex(),
                        token.getColumnIndex(),
                        "Directive '" + Directive.MACRO + "' requires a macro name followed by the list of macro parameters, if any"
                    ));
                    continue;
                }
                // Token after .macro must be an identifier
                token = tokens.get(index + 1);
                if (token.getType() != TokenType.IDENTIFIER) {
                    errors.add(new ErrorMessage(
                        token.getFilename(),
                        token.getLineIndex(),
                        token.getColumnIndex(),
                        "Directive '" + Directive.MACRO + "' expected a macro name, got: " + token
                    ));
                    continue;
                }

                String macroName = token.getLiteral();
                // Gather the list of macro parameters
                List<Token> macroParameters = new ArrayList<>();
                for (Token parameter : tokens.subList(index + 2, tokens.size())) {
                    if (parameter.getType() == TokenType.MACRO_PARAMETER || parameter.isSPIMStyleMacroParameter()) {
                        macroParameters.add(parameter);
                    }
                    else if (
                        parameter.getType() != TokenType.DELIMITER && parameter.getType() != TokenType.LEFT_PAREN
                        && parameter.getType() != TokenType.RIGHT_PAREN && parameter.getType() != TokenType.COMMENT
                    ) {
                        errors.add(new ErrorMessage(
                            parameter.getFilename(),
                            parameter.getLineIndex(),
                            parameter.getColumnIndex(),
                            "Directive '" + Directive.MACRO + "' expected a macro parameter, got: " + parameter
                        ));
                    }
                }

                // Start the macro definition
                this.currentMacro = new Macro(macroName, macroParameters);
                return;
            }
            // Check for .end_macro in case it is used incorrectly
            else if (token.getValue() == Directive.END_MACRO) {
                errors.add(new ErrorMessage(
                    token.getFilename(),
                    token.getLineIndex(),
                    token.getColumnIndex(),
                    "Directive '" + Directive.END_MACRO + "' must follow '" + Directive.MACRO + "'"
                ));
            }
        }

        // No preprocessing needed, so add the line without any processing
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
                        token.getFilename(),
                        token.getLineIndex(),
                        token.getColumnIndex()
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
