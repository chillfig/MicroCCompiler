package assembly.instructions;

/**
 * Class corresponding to Custom RISC-V instruction FMOVI.S
 * 
 * Parallel to IMOVF.S, used for converting float temporary to int
 */
public class Fmovis extends Instruction {

    /**
     * Initializes FMOVI.S instruction that prints FMOVI.S dest src
     * 
     * @param src1 source operand
     * @param dest destination operand
     */
    public Fmovis(String src, String dest) {
        super();
        this.src1 = src;
        this.dest = dest;
        this.oc = OpCode.FMOVIS;
    }

    /**
     * @return "FMOVI.S dest src"
     */
    public String toString() {
        return this.oc + " " + this.dest + ", " + this.src1;
    }  
}