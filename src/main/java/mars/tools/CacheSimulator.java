package mars.tools;

import mars.mips.hardware.Memory;
import mars.mips.hardware.MemoryConfigurations;
import mars.util.Binary;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

/*
Copyright (c) 2003-2011,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the 
"Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, 
distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject 
to the following conditions:

The above copyright notice and this permission notice shall be 
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
 */

/**
 * A data cache simulator for simulating and illustrating data cache performance.
 * <ul>
 * <li><b>Version 1.0</b> (16-18 October 2006)</li>
 * <li><b>Version 1.1</b> (7 November 2006)</li>
 * <li><b>Version 1.2</b> (23 December 2010) fixes a bug in the hit/miss animator under full or N-way set associative.
 *     It was animating the block of initial access (first block of set).  Now it animates the block of final access
 *     (where address found or stored). Also added log display to GUI (previously used {@link System#out}).</li>
 * </ul>
 *
 * @author Pete Sanderson
 */
public class CacheSimulator extends AbstractMarsTool {
    /**
     * Controls display of debugging info.
     */
    public boolean debug = false;

    // Major GUI components
    private JComboBox<Integer> cacheBlockSizeSelector;
    private JComboBox<Integer> cacheBlockCountSelector;
    private JComboBox<Integer> cacheSetSizeSelector;
    private JComboBox<PlacementPolicy> cachePlacementSelector;
    private JComboBox<ReplacementPolicy> cacheReplacementSelector;
    private JTextField memoryAccessCountDisplay;
    private JTextField cacheHitCountDisplay;
    private JTextField cacheMissCountDisplay;
    private JTextField cacheSizeDisplay;
    private JProgressBar cacheHitRateDisplay;
    private Animation animations;
    private JTextArea logDisplay;

    // Some GUI settings
    private final EmptyBorder emptyBorder = new EmptyBorder(4, 4, 4, 4);
    private final Font countFonts = new Font("Times", Font.BOLD, 12);

    // Values for Combo Boxes
    private static final int[] CACHE_BLOCK_SIZE_CHOICES = { 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048 };
    private static final int[] CACHE_BLOCK_COUNT_CHOICES = { 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048 };

    public enum PlacementPolicy {
        DIRECT("Direct Mapping"),
        FULL("Fully Associative"),
        SET("N-way Set Associative");

        public static final PlacementPolicy DEFAULT = DIRECT;

        private final String label;

        PlacementPolicy(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return this.label;
        }
    }

    public enum ReplacementPolicy {
        LRU("LRU"),
        RANDOM("Random");

        public static final ReplacementPolicy DEFAULT = LRU;

        private final String label;

        ReplacementPolicy(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return this.label;
        }
    }

    private static final int DEFAULT_CACHE_BLOCK_SIZE = 4;
    private static final int DEFAULT_CACHE_BLOCK_COUNT = 8;
    private static final int DEFAULT_CACHE_SET_SIZE_INDEX = 0;

    // Cache-related data structures
    private Integer[] cacheSetSizeChoices; // Will change dynamically based on the other selections
    private AbstractCache cache;
    private int memoryAccessCount;
    private int cacheHitCount;
    private int cacheMissCount;
    private double cacheHitRate;

    // RNG used for random replacement policy.  For testing, set seed for reproducible stream
    private final Random random = new Random(0);

    @Override
    public String getIdentifier() {
        return "cachesim";
    }

    /**
     * Required MarsTool method to return Tool name.
     *
     * @return Tool name.  MARS will display this in menu item.
     */
    @Override
    public String getDisplayName() {
        return "Data Cache Simulator";
    }

    @Override
    public String getVersion() {
        return "1.3";
    }

    /**
     * Method that constructs the main cache simulator display area.  It is organized vertically
     * into three major components: the cache configuration which an be modified
     * using combo boxes, the cache performance which is updated as the
     * attached MIPS program executes, and the runtime log which is optionally used
     * to display log of each cache access.
     *
     * @return The GUI component containing these three areas.
     */
    @Override
    protected JComponent buildMainDisplayArea() {
        // Overall structure of main UI (center)
        Box mainArea = Box.createVerticalBox();
        mainArea.add(this.buildOrganizationArea());
        mainArea.add(this.buildPerformanceArea());
        mainArea.add(this.buildLogArea());
        return mainArea;
    }

    private JComponent buildLogArea() {
        JPanel logPanel = new JPanel();
        TitledBorder ltb = new TitledBorder("Runtime Log");
        ltb.setTitleJustification(TitledBorder.CENTER);
        logPanel.setBorder(ltb);
        JCheckBox logShow = new JCheckBox("Enabled", this.debug);
        logShow.addItemListener(event -> {
            this.debug = event.getStateChange() == ItemEvent.SELECTED;
            resetLogDisplay();
            this.logDisplay.setEnabled(this.debug);
        });
        logPanel.add(logShow);
        this.logDisplay = new JTextArea(5, 70);
        this.logDisplay.setEnabled(this.debug);
        this.logDisplay.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        this.logDisplay.setToolTipText("Displays cache activity log if enabled");
        JScrollPane logScroll = new JScrollPane(this.logDisplay, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        logPanel.add(logScroll);
        return logPanel;
    }

    private JComponent buildOrganizationArea() {
        JPanel organization = new JPanel(new GridLayout(3, 2));
        organization.setBorder(BorderFactory.createTitledBorder(null, "Cache Organization", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));

        this.cachePlacementSelector = new JComboBox<>(PlacementPolicy.values());
        this.cachePlacementSelector.setEditable(false);
        this.cachePlacementSelector.setSelectedItem(PlacementPolicy.DEFAULT);
        this.cachePlacementSelector.addActionListener(event -> {
            this.updateCacheSetSizeSelector();
            this.reset();
        });

        this.cacheReplacementSelector = new JComboBox<>(ReplacementPolicy.values());
        this.cacheReplacementSelector.setEditable(false);
        this.cacheReplacementSelector.setSelectedItem(ReplacementPolicy.DEFAULT);

        this.cacheBlockSizeSelector = new JComboBox<>(Arrays.stream(CACHE_BLOCK_SIZE_CHOICES).boxed().toArray(Integer[]::new));
        this.cacheBlockSizeSelector.setEditable(false);
        this.cacheBlockSizeSelector.setSelectedItem(DEFAULT_CACHE_BLOCK_SIZE);
        this.cacheBlockSizeSelector.addActionListener(event -> {
            this.updateCacheSizeDisplay();
            this.reset();
        });
        this.cacheBlockCountSelector = new JComboBox<>(Arrays.stream(CACHE_BLOCK_COUNT_CHOICES).boxed().toArray(Integer[]::new));
        this.cacheBlockCountSelector.setEditable(false);
        this.cacheBlockCountSelector.setSelectedItem(DEFAULT_CACHE_BLOCK_COUNT);
        this.cacheBlockCountSelector.addActionListener(event -> {
            this.updateCacheSetSizeSelector();
            this.cache = this.createNewCache();
            this.resetCounts();
            this.updateDisplay();
            this.updateCacheSizeDisplay();
            this.animations.fillAnimationBoxWithCacheBlocks();
        });

        this.cacheSetSizeSelector = new JComboBox<>(this.cacheSetSizeChoices);
        this.cacheSetSizeSelector.setEditable(false);
        this.cacheSetSizeSelector.setSelectedIndex(DEFAULT_CACHE_SET_SIZE_INDEX);
        this.cacheSetSizeSelector.addActionListener(event -> reset());

        // All components for "Cache Organization" section
        JPanel placementPolicyRow = this.getPanelWithBorderLayout();
        placementPolicyRow.setBorder(this.emptyBorder);
        placementPolicyRow.add(new JLabel("Placement Policy "), BorderLayout.WEST);
        placementPolicyRow.add(this.cachePlacementSelector, BorderLayout.EAST);

        JPanel replacementPolicyRow = this.getPanelWithBorderLayout();
        replacementPolicyRow.setBorder(this.emptyBorder);
        replacementPolicyRow.add(new JLabel("Block Replacement Policy "), BorderLayout.WEST);
        replacementPolicyRow.add(this.cacheReplacementSelector, BorderLayout.EAST);

        JPanel cacheSetSizeRow = this.getPanelWithBorderLayout();
        cacheSetSizeRow.setBorder(this.emptyBorder);
        cacheSetSizeRow.add(new JLabel("Set size (blocks) "), BorderLayout.WEST);
        cacheSetSizeRow.add(this.cacheSetSizeSelector, BorderLayout.EAST);

        // Cachable address range "selection" removed for now...
//        JPanel cachableAddressesRow = getPanelWithBorderLayout();
//        cachableAddressesRow.setBorder(emptyBorder);
//        cachableAddressesRow.add(new JLabel("Cachable Addresses "),BorderLayout.WEST);
//        cachableAddressesDisplay = new JTextField("all data segment");
//        cachableAddressesDisplay.setEditable(false);
//        cachableAddressesDisplay.setHorizontalAlignment(JTextField.RIGHT);
//        cachableAddressesRow.add(cachableAddressesDisplay, BorderLayout.EAST);

        JPanel cacheNumberBlocksRow = this.getPanelWithBorderLayout();
        cacheNumberBlocksRow.setBorder(this.emptyBorder);
        cacheNumberBlocksRow.add(new JLabel("Number of blocks "), BorderLayout.WEST);
        cacheNumberBlocksRow.add(this.cacheBlockCountSelector, BorderLayout.EAST);

        JPanel cacheBlockSizeRow = this.getPanelWithBorderLayout();
        cacheBlockSizeRow.setBorder(this.emptyBorder);
        cacheBlockSizeRow.add(new JLabel("Cache block size (words) "), BorderLayout.WEST);
        cacheBlockSizeRow.add(this.cacheBlockSizeSelector, BorderLayout.EAST);

        JPanel cacheTotalSizeRow = this.getPanelWithBorderLayout();
        cacheTotalSizeRow.setBorder(this.emptyBorder);
        cacheTotalSizeRow.add(new JLabel("Cache size (bytes) "), BorderLayout.WEST);
        this.cacheSizeDisplay = new JTextField(8);
        this.cacheSizeDisplay.setHorizontalAlignment(JTextField.RIGHT);
        this.cacheSizeDisplay.setEditable(false);
        this.cacheSizeDisplay.setFont(this.countFonts);
        cacheTotalSizeRow.add(this.cacheSizeDisplay, BorderLayout.EAST);
        this.updateCacheSizeDisplay();

        // Lay 'em out in the grid...
        organization.add(placementPolicyRow);
        organization.add(cacheNumberBlocksRow);
        organization.add(replacementPolicyRow);
        organization.add(cacheBlockSizeRow);
//        organization.add(cachableAddressesRow);
        organization.add(cacheSetSizeRow);
        organization.add(cacheTotalSizeRow);
        return organization;
    }

    private JComponent buildPerformanceArea() {
        JPanel performance = new JPanel(new GridLayout(1, 2));
        performance.setBorder(BorderFactory.createTitledBorder(null, "Cache Performance", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));

        JPanel memoryAccessCountRow = this.getPanelWithBorderLayout();
        memoryAccessCountRow.setBorder(this.emptyBorder);
        memoryAccessCountRow.add(new JLabel("Memory Access Count "), BorderLayout.WEST);
        this.memoryAccessCountDisplay = new JTextField(10);
        this.memoryAccessCountDisplay.setHorizontalAlignment(JTextField.RIGHT);
        this.memoryAccessCountDisplay.setEditable(false);
        this.memoryAccessCountDisplay.setFont(this.countFonts);
        memoryAccessCountRow.add(this.memoryAccessCountDisplay, BorderLayout.EAST);

        JPanel cacheHitCountRow = this.getPanelWithBorderLayout();
        cacheHitCountRow.setBorder(this.emptyBorder);
        cacheHitCountRow.add(new JLabel("Cache Hit Count "), BorderLayout.WEST);
        this.cacheHitCountDisplay = new JTextField(10);
        this.cacheHitCountDisplay.setHorizontalAlignment(JTextField.RIGHT);
        this.cacheHitCountDisplay.setEditable(false);
        this.cacheHitCountDisplay.setFont(this.countFonts);
        cacheHitCountRow.add(this.cacheHitCountDisplay, BorderLayout.EAST);

        JPanel cacheMissCountRow = this.getPanelWithBorderLayout();
        cacheMissCountRow.setBorder(this.emptyBorder);
        cacheMissCountRow.add(new JLabel("Cache Miss Count "), BorderLayout.WEST);
        this.cacheMissCountDisplay = new JTextField(10);
        this.cacheMissCountDisplay.setHorizontalAlignment(JTextField.RIGHT);
        this.cacheMissCountDisplay.setEditable(false);
        this.cacheMissCountDisplay.setFont(this.countFonts);
        cacheMissCountRow.add(this.cacheMissCountDisplay, BorderLayout.EAST);

        JPanel cacheHitRateRow = this.getPanelWithBorderLayout();
        cacheHitRateRow.setBorder(this.emptyBorder);
        cacheHitRateRow.add(new JLabel("Cache Hit Rate "), BorderLayout.WEST);
        this.cacheHitRateDisplay = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
        this.cacheHitRateDisplay.setStringPainted(true);
        this.cacheHitRateDisplay.setForeground(new Color(61, 190, 255));
        this.cacheHitRateDisplay.setFont(this.countFonts);
        cacheHitRateRow.add(this.cacheHitRateDisplay, BorderLayout.EAST);

        this.resetCounts();
        this.updateDisplay();

        // Vertically align these 4 measures in a grid, then add to left column of main grid.
        JPanel performanceMeasures = new JPanel(new GridLayout(4, 1));
        performanceMeasures.add(memoryAccessCountRow);
        performanceMeasures.add(cacheHitCountRow);
        performanceMeasures.add(cacheMissCountRow);
        performanceMeasures.add(cacheHitRateRow);
        performance.add(performanceMeasures);

        // Let's try some animation on the right side...
        this.animations = new Animation();
        this.animations.fillAnimationBoxWithCacheBlocks();
        JPanel animationsPanel = new JPanel(new GridLayout(1, 2));
        Box animationsLabel = Box.createVerticalBox();
        JPanel tableTitle1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel tableTitle2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tableTitle1.add(new JLabel("Cache Block Table"));
        tableTitle2.add(new JLabel("(block 0 at top)"));
        animationsLabel.add(tableTitle1);
        animationsLabel.add(tableTitle2);
        Dimension colorKeyBoxSize = new Dimension(8, 8);

        JPanel emptyKey = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel emptyBox = new JPanel();
        emptyBox.setSize(colorKeyBoxSize);
        emptyBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        emptyKey.add(emptyBox);
        emptyKey.add(new JLabel(" = empty"));

        JPanel missBox = new JPanel();
        JPanel missKey = new JPanel(new FlowLayout(FlowLayout.LEFT));
        missBox.setSize(colorKeyBoxSize);
        missBox.setBackground(Animation.MISS_COLOR);
        missBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        missKey.add(missBox);
        missKey.add(new JLabel(" = miss"));

        JPanel hitKey = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel hitBox = new JPanel();
        hitBox.setSize(colorKeyBoxSize);
        hitBox.setBackground(Animation.HIT_COLOR);
        hitBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        hitKey.add(hitBox);
        hitKey.add(new JLabel(" = hit"));

        animationsLabel.add(emptyKey);
        animationsLabel.add(hitKey);
        animationsLabel.add(missKey);
        animationsLabel.add(Box.createVerticalGlue());
        animationsPanel.add(animationsLabel);
        animationsPanel.add(this.animations.getAnimationBox());

        performance.add(animationsPanel);
        return performance;
    }

    @Override
    protected void startObserving() {
        Memory.getInstance().addListener(
            this,
            Memory.getInstance().getAddress(MemoryConfigurations.DATA_LOW),
            Memory.getInstance().getAddress(MemoryConfigurations.DATA_HIGH)
        );
    }

    @Override
    protected void stopObserving() {
        Memory.getInstance().removeListener(this);
    }

    @Override
    public void memoryRead(int address, int length, int value, int wordAddress, int wordValue) {
        this.processMemoryAccess(wordAddress);
    }

    @Override
    public void memoryWritten(int address, int length, int value, int wordAddress, int wordValue) {
        this.processMemoryAccess(wordAddress);
    }

    /**
     * Apply caching policies and update display when connected MIPS program accesses (data) memory.
     */
    private void processMemoryAccess(int address) {
        this.memoryAccessCount++;
        CacheAccessResult cacheAccessResult = this.cache.isItAHitThenReadOnMiss(address);
        if (cacheAccessResult.isHit()) {
            this.cacheHitCount++;
            this.animations.showHit(cacheAccessResult.blockNumber);
        }
        else {
            this.cacheMissCount++;
            this.animations.showMiss(cacheAccessResult.blockNumber);
        }
        this.cacheHitRate = this.cacheHitCount / (double) this.memoryAccessCount;

        this.updateDisplay();
    }

    /**
     * Initialize all JComboBox choice structures not already initialized at declaration.
     * Also creates initial default cache object. Overrides inherited method that does nothing.
     */
    @Override
    protected void initializePreGUI() {
        this.cacheSetSizeChoices = this.determineSetSizeChoices(DEFAULT_CACHE_BLOCK_COUNT, PlacementPolicy.DEFAULT);
    }

    /**
     * The only post-GUI initialization is to create the initial cache object based on the default settings
     * of the various combo boxes. Overrides inherited method that does nothing.
     */
    @Override
    protected void initializePostGUI() {
        this.cache = this.createNewCache();
    }

    /**
     * Method to reset cache, counters and display when the Reset button selected.
     * Overrides inherited method that does nothing.
     */
    @Override
    protected void reset() {
        this.cache = this.createNewCache();
        this.resetCounts();
        this.updateDisplay();
        this.animations.reset();
        this.resetLogDisplay();
    }

    /**
     * Updates display immediately after each memory access is processed, after
     * cache configuration changes as needed, and after each execution step when Mars
     * is running in timed mode.  Overrides inherited method that does nothing.
     */
    private void updateDisplay() {
        this.updateMemoryAccessCountDisplay();
        this.updateCacheHitCountDisplay();
        this.updateCacheMissCountDisplay();
        this.updateCacheHitRateDisplay();
    }

    /**
     * Will determine range of choices for "set size in blocks", which is determined both by
     * the number of blocks in the cache and by placement policy.
     */
    private Integer[] determineSetSizeChoices(int cacheBlockCount, PlacementPolicy placementPolicy) {
        int[] choices;
        switch (placementPolicy) {
            case DIRECT:
                choices = new int[1];
                choices[0] = CACHE_BLOCK_COUNT_CHOICES[0]; // set size fixed at 1
                break;
            case SET:
                choices = new int[Arrays.binarySearch(CACHE_BLOCK_COUNT_CHOICES, cacheBlockCount) + 1];
                System.arraycopy(CACHE_BLOCK_COUNT_CHOICES, 0, choices, 0, choices.length);
                break;
            case FULL:   // 1 set total, so set size fixed at current number of blocks
            default:
                choices = new int[1];
                choices[0] = cacheBlockCount;
        }
        return Arrays.stream(choices).boxed().toArray(Integer[]::new);
    }

    /**
     * Update the Set Size combo box selection in response to other selections..
     */
    private void updateCacheSetSizeSelector() {
        this.cacheSetSizeSelector.setModel(new DefaultComboBoxModel<>(this.determineSetSizeChoices(
            this.cacheBlockCountSelector.getSelectedIndex(),
            (PlacementPolicy) Objects.requireNonNull(this.cachePlacementSelector.getSelectedItem())
        )));
    }

    /**
     * Create and return a new cache object based on current specs.
     */
    private AbstractCache createNewCache() {
        return new AnyCache(
            CACHE_BLOCK_COUNT_CHOICES[this.cacheBlockCountSelector.getSelectedIndex()],
            CACHE_BLOCK_SIZE_CHOICES[this.cacheBlockSizeSelector.getSelectedIndex()],
            (Integer) Objects.requireNonNull(this.cacheSetSizeSelector.getSelectedItem())
        );
    }

    private void resetCounts() {
        this.memoryAccessCount = 0;
        this.cacheHitCount = 0;
        this.cacheMissCount = 0;
        this.cacheHitRate = 0.0;
    }

    private void updateMemoryAccessCountDisplay() {
        this.memoryAccessCountDisplay.setText(Integer.toString(this.memoryAccessCount));
    }

    private void updateCacheHitCountDisplay() {
        this.cacheHitCountDisplay.setText(Integer.toString(this.cacheHitCount));
    }

    private void updateCacheMissCountDisplay() {
        this.cacheMissCountDisplay.setText(Integer.toString(this.cacheMissCount));
    }

    private void updateCacheHitRateDisplay() {
        this.cacheHitRateDisplay.setValue((int) Math.round(this.cacheHitRate * 100.0));
    }

    private void updateCacheSizeDisplay() {
        int cacheSize = CACHE_BLOCK_SIZE_CHOICES[this.cacheBlockSizeSelector.getSelectedIndex()]
            * CACHE_BLOCK_COUNT_CHOICES[this.cacheBlockCountSelector.getSelectedIndex()]
            * Memory.BYTES_PER_WORD;
        cacheSizeDisplay.setText(Integer.toString(cacheSize));
    }

    private JPanel getPanelWithBorderLayout() {
        return new JPanel(new BorderLayout(2, 2));
    }

    private void resetLogDisplay() {
        this.logDisplay.setText("");
    }

    private void writeLog(String text) {
        this.logDisplay.append(text);
        this.logDisplay.setCaretPosition(this.logDisplay.getDocument().getLength());
    }

    /**
     * Represents a block in the cache.  Since we are only simulating
     * cache performance, there's no need to actually store memory contents.
     */
    private static class CacheBlock {
        public boolean valid;
        public int tag;
        public int sizeInWords;
        public int mostRecentAccessTime;

        public CacheBlock(int sizeInWords) {
            this.valid = false;
            this.tag = 0;
            this.sizeInWords = sizeInWords;
            this.mostRecentAccessTime = -1;
        }
    }

    /**
     * Represents the outcome of a cache access.  There are two parts:
     * whether it was a hit or not, and in which block is the value stored.
     * In the case of a hit, the block associated with address.  In the case of
     * a miss, the block where new association is made.
     *
     * @author DPS 23-Dec-2010
     */
    private record CacheAccessResult(boolean isHit, int blockNumber) {}

    /**
     * Abstract Cache class.  Subclasses will implement specific policies.
     */
    private abstract static class AbstractCache {
        private final int numberOfBlocks;
        private final int blockSizeInWords;
        private final int setSizeInBlocks;
        private final int numberOfSets;
        protected CacheBlock[] blocks;

        protected AbstractCache(int numberOfBlocks, int blockSizeInWords, int setSizeInBlocks) {
            this.numberOfBlocks = numberOfBlocks;
            this.blockSizeInWords = blockSizeInWords;
            this.setSizeInBlocks = setSizeInBlocks;
            this.numberOfSets = numberOfBlocks / setSizeInBlocks;
            this.blocks = new CacheBlock[numberOfBlocks];
            this.reset();
        }

        // This will work regardless of placement. 
        // For direct map, #sets==#blocks
        // For full assoc, #sets==1 so anything % #sets == 0
        // For n-way assoc, it extracts the set bits in address.
        public int getSetNumber(int address) {
            return address / Memory.BYTES_PER_WORD / this.blockSizeInWords % this.numberOfSets;
        }

        // This will work regardless of placement policy (direct map, n-way or full assoc)
        public int getTag(int address) {
            return address / Memory.BYTES_PER_WORD / this.blockSizeInWords / this.numberOfSets;
        }

        // This will work regardless of placement policy (direct map, n-way or full assoc)
        // Returns absolute block offset into the cache.
        public int getFirstBlockToSearch(int address) {
            return this.getSetNumber(address) * this.setSizeInBlocks;
        }

        // This will work regardless of placement policy (direct map, n-way or full assoc)
        // Returns absolute block offset into the cache.
        public int getLastBlockToSearch(int address) {
            return this.getFirstBlockToSearch(address) + this.setSizeInBlocks - 1;
        }

        /**
         * Reset the cache contents.
         */
        public void reset() {
            for (int block = 0; block < this.numberOfBlocks; block++) {
                this.blocks[block] = new CacheBlock(this.blockSizeInWords);
            }
            System.gc(); // Scoop 'em up now
        }

        // Subclass must implement this according to its policies
        public abstract CacheAccessResult isItAHitThenReadOnMiss(int address);
    }

    // Implements any of the well-known cache organizations.  Physical memory
    // address is partitioned depending on organization:
    //    Direct Mapping:    [ tag | block | word | byte ]
    //    Fully Associative: [ tag | word | byte ]
    //    Set Associative:   [ tag | set | word | byte ]
    //
    // Bit lengths of each part are determined as follows:
    // Direct Mapping:
    //   byte  = log2 of #bytes in a word (typically 4)
    //   word  = log2 of #words in a block
    //   block = log2 of #blocks in the cache
    //   tag   = #bytes in address - (byte+word+block)
    // Fully Associative:
    //   byte  = log2 of #bytes in a word (typically 4)
    //   word  = log2 of #words in a block
    //   tag   = #bytes in address - (byte+word)
    // Set Associative:
    //   byte  = log2 of #bytes in a word (typically 4)
    //   word  = log2 of #words in a block
    //   set   = log2 of #sets in the cache
    //   tag   = #bytes in address - (byte+word+set)
    //
    // Direct Mapping (1 way set associative):
    // The block value for a given address identifies its block index into the cache.
    // That's why its called "direct mapped."  This is the only cache block it can
    // occupy.  If that cache block is empty or if it is occupied by a different tag,
    // this is a MISS.  If that cache block is occupied by the same tag, this is a HIT.
    // There is no replacement policy: upon a cache miss of an occupied block, the old
    // block is written out (unless write-through) and the new one read in.
    // Those actions are not simulated here.
    //
    // Fully Associative:
    // There is one set, and very tag has to be searched before determining hit or miss.
    // If tag is matched, it is a hit.  If tag is not matched and there is at least one
    // empty block, it is a miss and the new tag will occupy it.  If tag is not matched
    // and every block is occupied, it is a miss and one of the occupied blocks will be
    // selected for removal and the new tag will replace it.
    //
    // n-way Set Associative:
    // Each set consists of n blocks, and the number of sets in the cache is total number
    // of blocks divided by n.  The set bits in the address will identify which set to
    // search, and every tag in that set has to be searched before determining hit or miss.
    // If tag is matched, it is a hit.  If tag is not matched and there is at least one
    // empty block, it is a miss and the new tag will occupy it.  If tag is not matched
    // and every block is occupied, it is a miss and one of the occupied blocks will be
    // selected for removal and the new tag will replace it.
    //
    private class AnyCache extends AbstractCache {
        public AnyCache(int numberOfBlocks, int blockSizeInWords, int setSizeInBlocks) {
            super(numberOfBlocks, blockSizeInWords, setSizeInBlocks);
        }

        private enum CacheResultReason {
            SET_FULL,
            HIT,
            MISS,
        }

        /**
         * This method works for any of the placement policies:
         * direct mapped, full associative or n-way set associative.
         */
        @Override
        public CacheAccessResult isItAHitThenReadOnMiss(int address) {
            CacheResultReason result = CacheResultReason.SET_FULL;
            int firstBlock = this.getFirstBlockToSearch(address);
            int lastBlock = this.getLastBlockToSearch(address);
            if (CacheSimulator.this.debug) {
                CacheSimulator.this.writeLog("(" + CacheSimulator.this.memoryAccessCount + ") address: "
                    + Binary.intToHexString(address) + " (tag " + Binary.intToHexString(this.getTag(address)) + ") "
                    + " block range: " + firstBlock + "-" + lastBlock + "\n");
            }
            CacheBlock block;
            int blockNumber;
            // Will do a sequential instead of associative search!
            for (blockNumber = firstBlock; blockNumber <= lastBlock; blockNumber++) {
                block = this.blocks[blockNumber];
                if (CacheSimulator.this.debug) {
                    CacheSimulator.this.writeLog("   trying block " + blockNumber + ((block.valid)
                        ? " tag " + Binary.intToHexString(block.tag)
                        : " empty"));
                }
                if (block.valid && block.tag == getTag(address)) {//  it's a hit!
                    if (CacheSimulator.this.debug) {
                        CacheSimulator.this.writeLog(" -- HIT\n");
                    }
                    result = CacheResultReason.HIT;
                    block.mostRecentAccessTime = CacheSimulator.this.memoryAccessCount;
                    break;
                }
                if (!block.valid) {
                    // It's a miss but I got it now because it is empty!
                    if (CacheSimulator.this.debug) {
                        CacheSimulator.this.writeLog(" -- MISS\n");
                    }
                    result = CacheResultReason.MISS;
                    block.valid = true;
                    block.tag = this.getTag(address);
                    block.mostRecentAccessTime = CacheSimulator.this.memoryAccessCount;
                    break;
                }
                if (CacheSimulator.this.debug) {
                    CacheSimulator.this.writeLog(" -- OCCUPIED\n");
                }
            }
            if (result == CacheResultReason.SET_FULL) {
                // Select one to replace and replace it...
                if (CacheSimulator.this.debug) {
                    CacheSimulator.this.writeLog("   MISS due to FULL SET");
                }
                int blockToReplace = this.selectBlockToReplace(firstBlock, lastBlock);
                block = blocks[blockToReplace];
                block.tag = this.getTag(address);
                block.mostRecentAccessTime = CacheSimulator.this.memoryAccessCount;
                blockNumber = blockToReplace;
            }
            return new CacheAccessResult(result == CacheResultReason.HIT, blockNumber);
        }

        /**
         * Call this if all blocks in the set are full.  If the set contains more than one block,
         * It will pick on to replace based on selected replacement policy.
         */
        private int selectBlockToReplace(int first, int last) {
            int replaceBlock = first;
            if (first != last) {
                switch ((ReplacementPolicy) Objects.requireNonNull(CacheSimulator.this.cacheReplacementSelector.getSelectedItem())) {
                    case RANDOM:
                        replaceBlock = first + CacheSimulator.this.random.nextInt(last - first + 1);
                        if (CacheSimulator.this.debug) {
                            CacheSimulator.this.writeLog(" -- Random replace block " + replaceBlock + "\n");
                        }
                        break;
                    case LRU:
                    default:
                        int leastRecentAccessTime = CacheSimulator.this.memoryAccessCount; // All of them have to be less than this
                        for (int block = first; block <= last; block++) {
                            if (this.blocks[block].mostRecentAccessTime < leastRecentAccessTime) {
                                leastRecentAccessTime = this.blocks[block].mostRecentAccessTime;
                                replaceBlock = block;
                            }
                        }
                        if (CacheSimulator.this.debug) {
                            CacheSimulator.this.writeLog(" -- LRU replace block " + replaceBlock + "; unused since (" + leastRecentAccessTime + ")\n");
                        }
                        break;
                }
            }
            return replaceBlock;
        }
    }

    /**
     * Class to display animated cache
     */
    private class Animation {
        public static final Color HIT_COLOR = Color.GREEN;
        public static final Color MISS_COLOR = Color.RED;

        private final Box animation;
        private JTextField[] blocks;

        public Animation() {
            this.animation = Box.createVerticalBox();
        }

        public Box getAnimationBox() {
            return this.animation;
        }

        public void showHit(int blockNum) {
            this.blocks[blockNum].setBackground(HIT_COLOR);
        }

        public void showMiss(int blockNum) {
            this.blocks[blockNum].setBackground(MISS_COLOR);
        }

        public void reset() {
            for (JTextField block : this.blocks) {
                block.setBackground(null);
            }
        }

        /**
         * Initialize animation of cache blocks.
         */
        private void fillAnimationBoxWithCacheBlocks() {
            this.animation.setVisible(false);
            this.animation.removeAll();
            int numberOfBlocks = CACHE_BLOCK_COUNT_CHOICES[CacheSimulator.this.cacheBlockCountSelector.getSelectedIndex()];
            int totalVerticalPixels = 128;
            int blockPixelHeight = (numberOfBlocks > totalVerticalPixels) ? 1 : totalVerticalPixels / numberOfBlocks;
            int blockPixelWidth = 40;
            Dimension blockDimension = new Dimension(blockPixelWidth, blockPixelHeight);
            this.blocks = new JTextField[numberOfBlocks];
            for (int block = 0; block < numberOfBlocks; block++) {
                this.blocks[block] = new JTextField();
                this.blocks[block].setEditable(false);
                this.blocks[block].setBackground(null);
                this.blocks[block].setSize(blockDimension);
                this.blocks[block].setPreferredSize(blockDimension);
                this.animation.add(this.blocks[block]);
            }
            this.animation.repaint();
            this.animation.setVisible(true);
        }
    }
}
