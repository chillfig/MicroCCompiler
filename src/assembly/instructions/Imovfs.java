package assembly.instructions;

/**
 * Class corresponding to Custom RISC-V instruction IMOVF.S
 * 
 * Parallel to FMOVI.S, used for converting int temporary to float
 */
public class Imovfs extends Instruction {

    /**
     * Initializes IMOVF.S instruction that prints IMOVF.S dest src
     * 
     * @param src1 source operand
     * @param dest destination operand
     */
    public Imovfs(String src, String dest) {
        super();
        this.src1 = src;
        this.dest = dest;
        this.oc = OpCode.IMOVFS;
    }

    /**
     * @return "IMOVF.S dest src"
     */
    public String toString() {
        return this.oc + " " + this.dest + ", " + this.src1;
    }
}