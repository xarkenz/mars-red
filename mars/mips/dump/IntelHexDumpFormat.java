package mars.mips.dump;

import mars.Globals;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Intel's Hex memory initialization format
 *
 * @author Leo Alterman
 * @version July 2011
 */

public class IntelHexDumpFormat extends AbstractDumpFormat {

    /**
     * Constructor.  File extention is "hex".
     */
    public IntelHexDumpFormat() {
        super("Intel hex format", "HEX", "Written as Intel Hex Memory File", "hex");
    }

    /**
     * Write MIPS memory contents according to the Memory Initialization File
     * (MIF) specification.
     *
     * @param file         File in which to store MIPS memory contents.
     * @param firstAddress first (lowest) memory address to dump.  In bytes but
     *                     must be on word boundary.
     * @param lastAddress  last (highest) memory address to dump.  In bytes but
     *                     must be on word boundary.  Will dump the word that starts at this address.
     * @throws AddressErrorException if firstAddress is invalid or not on a word boundary.
     * @throws IOException           if error occurs during file output.
     */
    public void dumpMemoryRange(File file, int firstAddress, int lastAddress) throws AddressErrorException, IOException {
        try (PrintStream out = new PrintStream(new FileOutputStream(file))) {
            for (int address = firstAddress; address <= lastAddress; address += Memory.WORD_LENGTH_BYTES) {
                // TODO: This can probably be replaced with a single call to String.format() -Sean Clarke
                Integer wordOrNull = Globals.memory.getRawWordOrNull(address);
                if (wordOrNull == null) {
                    break;
                }
                int word = wordOrNull;
                StringBuilder string = new StringBuilder(Integer.toHexString(word));
                while (string.length() < 8) {
                    string.insert(0, '0');
                }
                StringBuilder addr = new StringBuilder(Integer.toHexString(address - firstAddress));
                while (addr.length() < 4) {
                    addr.insert(0, '0');
                }
                String chksum;
                int tmp_chksum = 0;
                tmp_chksum += 4;
                tmp_chksum += 0xFF & (address - firstAddress);
                tmp_chksum += 0xFF & ((address - firstAddress) >> 8);
                tmp_chksum += 0xFF & word;
                tmp_chksum += 0xFF & (word >> 8);
                tmp_chksum += 0xFF & (word >> 16);
                tmp_chksum += 0xFF & (word >> 24);
                tmp_chksum = tmp_chksum % 256;
                tmp_chksum = ~tmp_chksum + 1;
                chksum = Integer.toHexString(0xFF & tmp_chksum);
                if (chksum.length() == 1) {
                    chksum = '0' + chksum;
                }
                String finalstr = ":04" + addr + "00" + string + chksum;
                out.println(finalstr.toUpperCase());
            }
            out.println(":00000001FF");
        }
    }
}
