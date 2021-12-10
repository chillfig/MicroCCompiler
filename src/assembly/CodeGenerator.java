package assembly;

import java.util.List;

import compiler.Scope.InnerType;
import compiler.Scope.SymbolTableEntry;
import ast.visitor.AbstractASTVisitor;

import ast.*;
import assembly.instructions.*;
import compiler.Scope;

public class CodeGenerator extends AbstractASTVisitor<CodeObject> {

	int intRegCount;
	int floatRegCount;
	static final public char intTempPrefix = 't';
	static final public char floatTempPrefix = 'f';
	
	int loopLabel;
	int elseLabel;
	int outLabel;

	String currFunc;
	
	public CodeGenerator() {
		loopLabel = 0;
		elseLabel = 0;
		outLabel = 0;
		intRegCount = 0;		
		floatRegCount = 0;
	}

	public int getIntRegCount() {
		return intRegCount;
	}

	public int getFloatRegCount() {
		return floatRegCount;
	}
	
	/**
	 * Generate code for Variables
	 * 
	 * Create a code object that just holds a variable
	 * 
	 * Important: add a pointer from the code object to the symbol table entry
	 *            so we know how to generate code for it later (we'll need to find
	 *            the address)
	 * 
	 * Mark the code object as holding a variable, and also as an lval
	 */
	@Override
	protected CodeObject postprocess(VarNode node) {
		
		Scope.SymbolTableEntry sym = node.getSymbol();
		
		CodeObject co = new CodeObject(sym);
		co.lval = true;
		co.type = node.getType();

		return co;
	}

	/** Generate code for IntLiterals
	 * 
	 * Use load immediate instruction to do this.
	 */
	@Override
	protected CodeObject postprocess(IntLitNode node) {
		CodeObject co = new CodeObject();
		
		//Load an immediate into a register
		//The li and la instructions are the same, but it's helpful to distinguish
		//for readability purposes.
		//li tmp' value
		Instruction i = new Li(generateTemp(Scope.InnerType.INT), node.getVal());

		co.code.add(i); //add this instruction to the code object
		co.lval = false; //co holds an rval -- data
		co.temp = i.getDest(); //temp is in destination of li
		co.type = node.getType();

		return co;
	}

	/** Generate code for FloatLiteras
	 * 
	 * Use load immediate instruction to do this.
	 */
	@Override
	protected CodeObject postprocess(FloatLitNode node) {
		CodeObject co = new CodeObject();
		
		//Load an immediate into a regisster
		//The li and la instructions are the same, but it's helpful to distinguish
		//for readability purposes.
		//li tmp' value
		Instruction i = new FImm(generateTemp(Scope.InnerType.FLOAT), node.getVal());

		co.code.add(i); //add this instruction to the code object
		co.lval = false; //co holds an rval -- data
		co.temp = i.getDest(); //temp is in destination of li
		co.type = node.getType();

		return co;
	}

	/**
	 * Generate code for binary operations.
	 * 
	 * Step 0: create new code object
	 * Step 1: add code from left child
	 * Step 1a: if left child is an lval, add a load to get the data
	 * Step 2: add code from right child
	 * Step 2a: if right child is an lval, add a load to get the data
	 * Step 3: generate binary operation using temps from left and right
	 * 
	 * Don't forget to update the temp and lval fields of the code object!
	 * 	   Hint: where is the result stored? Is this data or an address?
	 * 
	 */
	@Override
	protected CodeObject postprocess(BinaryOpNode node, CodeObject left, CodeObject right) {
		// Step 0
		CodeObject co = new CodeObject();
			
		// Step 1a:
		//if left is an lval, load from it
		if (left.lval == true) {
			left = rvalify(left);
		}

		// Step 1: 
		co.code.addAll(left.code);

		// Step 2a 
		//if right is an lval, load from it
		if (right.lval == true){
			right = rvalify(right);
		}

		// Step 2
		co.code.addAll(right.code);

		// STEP 7 option 2:: check for implicit converisons
		Instruction implicitConvert = null;
		if (left.getType().type == Scope.InnerType.INT && right.getType().type == Scope.InnerType.FLOAT){
			implicitConvert = new Imovfs(left.temp, generateTemp(Scope.InnerType.FLOAT));
			co.code.add(implicitConvert);
			left.temp = co.code.getLast().getDest();
			left.type = new Scope.Type(Scope.InnerType.FLOAT);
		}else if (left.getType().type == Scope.InnerType.FLOAT && right.getType().type == Scope.InnerType.INT){
			implicitConvert = new Imovfs(right.temp, generateTemp(Scope.InnerType.FLOAT));
			co.code.add(implicitConvert);
			right.temp = co.code.getLast().getDest();			
			right.type = new Scope.Type(Scope.InnerType.FLOAT);
		}
		
		// Step 3
		Instruction ins = null;
		switch (node.getOp()){
			case ADD: 
				if (left.getType().type == Scope.InnerType.PTR){
					ins = new Add(left.temp, right.temp, generateTemp(Scope.InnerType.PTR));
				}	
				if (left.getType().type == Scope.InnerType.INT){
					ins = new Add(left.temp, right.temp, generateTemp(Scope.InnerType.INT));
				} 
				if (left.getType().type == Scope.InnerType.FLOAT){
					ins = new FAdd(left.temp, right.temp, generateTemp(Scope.InnerType.FLOAT));
				}
				break;
			case SUB: 
				if (left.getType().type == Scope.InnerType.PTR){
					ins = new Sub(left.temp, right.temp, generateTemp(Scope.InnerType.PTR));
				}		
				if (left.getType().type == Scope.InnerType.INT){
					ins = new Sub(left.temp, right.temp, generateTemp(Scope.InnerType.INT));
				} 
				if (left.getType().type == Scope.InnerType.FLOAT){
					ins = new FSub(left.temp, right.temp, generateTemp(Scope.InnerType.FLOAT));
				}
				break;
			case MUL: 
				if (left.getType().type == Scope.InnerType.PTR){
					ins = new Mul(left.temp, right.temp, generateTemp(Scope.InnerType.PTR));
				}		
				if (left.getType().type == Scope.InnerType.INT){
					ins = new Mul(left.temp, right.temp, generateTemp(Scope.InnerType.INT));
				} 
				if (left.getType().type == Scope.InnerType.FLOAT){
					ins = new FMul(left.temp, right.temp, generateTemp(Scope.InnerType.FLOAT));
				}
				break;
			case DIV: 
				if (left.getType().type == Scope.InnerType.PTR){
					ins = new Div(left.temp, right.temp, generateTemp(Scope.InnerType.PTR));
				}		
				if (left.getType().type == Scope.InnerType.INT){
					ins = new Div(left.temp, right.temp, generateTemp(Scope.InnerType.INT));
				} 
				if (left.getType().type == Scope.InnerType.FLOAT){
					ins = new FDiv(left.temp, right.temp, generateTemp(Scope.InnerType.FLOAT));
				}
				break;
		}
		co.code.add(ins);			//add this instruction to the code object
		co.lval = false;			//co holds an rval -- data
		co.temp = ins.getDest();	//temp is in destination of li
		co.type = left.getType();
		/* FILL IN FROM STEP 2 */

		return co;
	}

	/**
	 * Generate code for unary operations.
	 * 
	 * Step 0: create new code object
	 * Step 1: add code from child expression
	 * Step 1a: if child is an lval, add a load to get the data
	 * Step 2: generate instruction to perform unary operation
	 * 
	 * Don't forget to update the temp and lval fields of the code object!
	 * 	   Hint: where is the result stored? Is this data or an address?
	 * 
	 */
	@Override
	protected CodeObject postprocess(UnaryOpNode node, CodeObject expr) {
		
		CodeObject co = new CodeObject();
		Instruction ins = null;
		// Step 1a: 
		if (expr.lval == true){
			expr = rvalify(expr);
		}
		// Step 1:
		co.code.addAll(expr.code);

		// Step 2:
		if (node.getType().type == Scope.InnerType.PTR){
			ins = new Neg(expr.temp, generateTemp(Scope.InnerType.PTR));
		} 
		if (node.getType().type == Scope.InnerType.INT){
			ins = new Neg(expr.temp, generateTemp(Scope.InnerType.INT));
		} 
		if (node.getType().type == Scope.InnerType.FLOAT){
			ins = new FNeg(expr.temp, generateTemp(Scope.InnerType.FLOAT));
		}
		// Update temp and lval
		co.code.add(ins);			//add this instrcution to the code object
		co.lval = false;			//co holds and reval which is data
		co.temp = ins.getDest(); 	//temp is in destination of ins
		co.type = node.getType();
		/* FILL IN FROM STEP 2 */

		return co;
	}

	/**
	 * Generate code for assignment statements
	 * 
	 * Step 0: create new code object
	 * Step 1: if LHS is a variable, generate a load instruction to get the address into a register
	 * Step 1a: add code from LHS of assignment (make sure it results in an lval!)
	 * Step 2: add code from RHS of assignment
	 * Step 2a: if right child is an lval, add a load to get the data
	 * Step 3: generate store
	 * 
	 * Hint: it is going to be easiest to just generate a store with a 0 immediate
	 * offset, and the complete store address in a register:
	 * 
	 * sw rhs 0(lhs)
	 */
	@Override
	protected CodeObject postprocess(AssignNode node, CodeObject left,
			CodeObject right) {
		
		Instruction ins = null;
		//Step 0		
		CodeObject co = new CodeObject();

		assert(left.lval == true); //left hand side had better hold an address

		//Step 1a
		if (left.isVar() == true) {
			left.code.addAll(generateAddrFromVariable(left));
			left.temp = left.code.getLast().getDest();
		}

		//Step 1b
		co.code.addAll(left.code);

		//Step 2a
		if (right.lval == true) {
			right = rvalify(right);
		}

		//Step 2 
		co.code.addAll(right.code);
		
		// STEP 7 option 2:: check for implicit converisons
		Instruction implicitConvert = null;
		if (left.getType().type == Scope.InnerType.INT && right.getType().type == Scope.InnerType.FLOAT){
			implicitConvert = new Fmovis(right.temp, generateTemp(Scope.InnerType.INT));
			co.code.add(implicitConvert);
			right.temp = co.code.getLast().getDest();
		}else if (left.getType().type == Scope.InnerType.FLOAT && right.getType().type == Scope.InnerType.INT){
			implicitConvert = new Imovfs(right.temp, generateTemp(Scope.InnerType.FLOAT));
			co.code.add(implicitConvert);
			right.temp = co.code.getLast().getDest();
		}
		

		//Step 3
		if (left.getType().type == Scope.InnerType.INT || left.getType().type == Scope.InnerType.PTR){
			ins = new Sw(right.temp, left.temp, "0");
		} 
		if ((left.getType().type == Scope.InnerType.FLOAT)){
			ins = new Fsw(right.temp, left.temp, "0");
		}
		
		co.code.add(ins);			//add this instruction to the code object
		co.lval = false;			//doesn't matter
		co.temp = ins.getDest();	//set to null to trigger errors
		co.type = left.getType();	//set to null to trigger errors
		/* FILL IN FROM STEP 2 */
		
		return co;
	}

	/**
	 * Add together all the lists of instructions generated by the children
	 */
	@Override
	protected CodeObject postprocess(StatementListNode node,
			List<CodeObject> statements) {
		CodeObject co = new CodeObject();
		//add the code from each individual statement
		for (CodeObject subcode : statements) {
			co.code.addAll(subcode.code);
		}
		co.type = null; //set to null to trigger errors
		return co;
	}
	
	/**
	 * Generate code for read
	 * 
	 * Step 0: create new code object
	 * Step 1: add code from VarNode (make sure it's an lval)
	 * Step 2: generate GetI instruction, storing into temp
	 * Step 3: generate store, to store temp in variable
	 */
	@Override
	protected CodeObject postprocess(ReadNode node, CodeObject var) {
		
		//Step 0
		CodeObject co = new CodeObject();

		//Generating code for read(id)
		assert(var.getSTE() != null); //var had better be a variable

		InstructionList il = new InstructionList();
		switch(node.getType().type) {
			case INT: 
				//Code to generate if INT:
				//geti tmp
				//if var is global: la tmp', <var>; sw tmp 0(tmp')
				//if var is local: sw tmp offset(fp)
				Instruction geti = new GetI(generateTemp(Scope.InnerType.INT));
				il.add(geti);
				InstructionList store = new InstructionList();
				if (var.getSTE().isLocal()) {
					store.add(new Sw(geti.getDest(), "fp", String.valueOf(var.getSTE().addressToString())));
				} else {
					store.addAll(generateAddrFromVariable(var));
					store.add(new Sw(geti.getDest(), store.getLast().getDest(), "0"));
				}
				il.addAll(store);
				break;
			case FLOAT:
				//Code to generate if FLOAT:
				//getf tmp
				//if var is global: la tmp', <var>; fsw tmp 0(tmp')
				//if var is local: fsw tmp offset(fp)
				Instruction getf = new GetF(generateTemp(Scope.InnerType.FLOAT));
				il.add(getf);
				InstructionList fstore = new InstructionList();
				if (var.getSTE().isLocal()) {
					fstore.add(new Fsw(getf.getDest(), "fp", String.valueOf(var.getSTE().addressToString())));
				} else {
					fstore.addAll(generateAddrFromVariable(var));
					fstore.add(new Fsw(getf.getDest(), fstore.getLast().getDest(), "0"));
				}
				il.addAll(fstore);
				break;
			default:
				throw new Error("Shouldn't read into other variable");
		}
		
		co.code.addAll(il);

		co.lval = false; //doesn't matter
		co.temp = null; //set to null to trigger errors
		co.type = null; //set to null to trigger errors

		return co;
	}

	/**
	 * Generate code for print
	 * 
	 * Step 0: create new code object
	 * 
	 * If printing a string:
	 * Step 1: add code from expression to be printed (make sure it's an lval)
	 * Step 2: generate a PutS instruction printing the result of the expression
	 * 
	 * If printing an integer:
	 * Step 1: add code from the expression to be printed
	 * Step 1a: if it's an lval, generate a load to get the data
	 * Step 2: Generate PutI that prints the temporary holding the expression
	 */
	@Override
	protected CodeObject postprocess(WriteNode node, CodeObject expr) {
		CodeObject co = new CodeObject();

		//generating code for write(expr)

		//for strings, we expect a variable
		if (node.getWriteExpr().getType().type == Scope.InnerType.STRING) {
			//Step 1:
			assert(expr.getSTE() != null);
			
			System.out.println("; generating code to print " + expr.getSTE());

			//Get the address of the variable
			InstructionList addrCo = generateAddrFromVariable(expr);
			co.code.addAll(addrCo);

			//Step 2:
			Instruction write = new PutS(addrCo.getLast().getDest());
			co.code.add(write);
		} else {
			//Step 1a:
			//if expr is an lval, load from it
			if (expr.lval == true) {
				expr = rvalify(expr);
			}
			
			//Step 1:
			co.code.addAll(expr.code);

			//Step 2:
			//if type of writenode is int, use puti, if float, use putf
			Instruction write = null;
			switch(node.getWriteExpr().getType().type) {
			case STRING: throw new Error("Shouldn't have a STRING here");
			case INT: 
			case PTR: //should work the same way for pointers
				write = new PutI(expr.temp); break;
			case FLOAT: write = new PutF(expr.temp); break;
			default: throw new Error("WriteNode has a weird type");
			}

			co.code.add(write);
		}

		co.lval = false; //doesn't matter
		co.temp = null; //set to null to trigger errors
		co.type = null; //set to null to trigger errors

		return co;
	}

	/**
	 * FILL IN FROM STEP 3
	 * 
	 * Generating an instruction sequence for a conditional expression
	 * 
	 * Implement this however you like. One suggestion:
	 *
	 * Create the code for the left and right side of the conditional, but defer
	 * generating the branch until you process IfStatementNode or WhileNode (since you
	 * do not know the labels yet). Modify CodeObject so you can save the necessary
	 * information to generate the branch instruction in IfStatementNode or WhileNode
	 * 
	 * Alternate idea 1:
	 * 
	 * Don't do anything as part of CodeGenerator. Create a new visitor class
	 * that you invoke *within* your processing of IfStatementNode or WhileNode
	 * 
	 * Alternate idea 2:
	 * 
	 * Create the branch instruction in this function, then tweak it as necessary in
	 * IfStatementNode or WhileNode
	 * 
	 * Hint: you may need to preserve extra information in the returned CodeObject to
	 * make sure you know the type of branch code to generate (int vs float)
	 */
	@Override
	protected CodeObject postprocess(CondNode node, CodeObject left, CodeObject right) {
		CodeObject co = new CodeObject();

		//Step 1:
		if (left.lval == true) {
			left = rvalify(left);
		}
		co.code.addAll(left.code);
		
		//Step 2:
		if (right.lval == true) {
			right = rvalify(right);
		}
		co.code.addAll(right.code);
		
		//Step 3:
		co.tempLeft = left.temp;
		co.tempRight = right.temp;
		co.lval = false;
		co.type = left.getType();

		return co;
	}

	/**
	 * FILL IN FROM STEP 3
	 * 
	 * Step 0: Create code object
	 * 
	 * Step 1: generate labels
	 * 
	 * Step 2: add code from conditional expression
	 * 
	 * Step 3: create branch statement (if not created as part of step 2)
	 * 			don't forget to generate correct branch based on type
	 * 
	 * Step 4: generate code
	 * 		<cond code>
	 *		<flipped branch> elseLabel
	 *		<then code>
	 *		j outLabel
	 *		elseLabel:
	 *		<else code>
	 *		outLabel:
	 *
	 * Step 5 insert code into code object in appropriate order.
	 */
	@Override
	protected CodeObject postprocess(IfStatementNode node, CodeObject cond, CodeObject tlist, CodeObject elist) {
		//Step 0:
		CodeObject co = new CodeObject();

		//Step 1:
		String outputLabel = generateOutLabel();
		String elseLabel = generateElseLabel();
		String branchLabel = outputLabel;
		if(elist != null) {
			branchLabel = elseLabel;
		}
		
		//Step 2:
		co.code.addAll(cond.code);		//<cond code>
		
		//Step 3:
		Instruction branchTemp = null;
		Instruction floatTemp = null;
		String tempRegister;
		
		switch(node.getCondExpr().getReversedOp()) {
			case GT:
			//Reversed operation for branch instruction LE
			//if(left <= right)
			if(cond.getType().type == Scope.InnerType.INT  || cond.getType().type == Scope.InnerType.PTR) {
				branchTemp = new Bgt(cond.tempLeft, cond.tempRight, branchLabel);
			}
			if(cond.getType().type == Scope.InnerType.FLOAT) {
				tempRegister = generateTemp(Scope.InnerType.INT);
				floatTemp = new Flt(cond.tempRight, cond.tempLeft, tempRegister);
				branchTemp = new Bne(tempRegister, "x0", branchLabel);
			}
			break;
			case GE:
			//Reversed operation for branch instruction LT
			//if(left < right)
			if(cond.getType().type == Scope.InnerType.INT || cond.getType().type == Scope.InnerType.PTR) {
				branchTemp = new Bge(cond.tempLeft, cond.tempRight, branchLabel);
			}
			if(cond.getType().type == Scope.InnerType.FLOAT) {
				tempRegister = generateTemp(Scope.InnerType.INT);
				floatTemp = new Fle(cond.tempRight, cond.tempLeft, tempRegister);
				branchTemp = new Bne(tempRegister, "x0", branchLabel);
			}
			break;
			case LT:
			//Reversed operation for branch instruction GE
			//if(left >= right)
			if(cond.getType().type == Scope.InnerType.INT || cond.getType().type == Scope.InnerType.PTR) {
				branchTemp = new Blt(cond.tempLeft, cond.tempRight, branchLabel);
			}
			if(cond.getType().type == Scope.InnerType.FLOAT) {
				tempRegister = generateTemp(Scope.InnerType.INT);
				floatTemp = new Flt(cond.tempLeft, cond.tempRight, tempRegister);
				branchTemp = new Bne(tempRegister, "x0", branchLabel);
			}
			break;
			case LE:
			//Reversed operation for branch instruction GT
			//if(left > right)
			if(cond.getType().type == Scope.InnerType.INT || cond.getType().type == Scope.InnerType.PTR) {
				branchTemp = new Ble(cond.tempLeft, cond.tempRight, branchLabel);
			}
			if(cond.getType().type == Scope.InnerType.FLOAT) {
				tempRegister = generateTemp(Scope.InnerType.INT);
				floatTemp = new Fle(cond.tempLeft, cond.tempRight, tempRegister);
				branchTemp = new Bne(tempRegister, "x0", branchLabel);
			}
			break;
			case NE:
			//Reversed operation for branch instruction EQ
			//if(left == right)
			if(cond.getType().type == Scope.InnerType.INT || cond.getType().type == Scope.InnerType.PTR) {
				branchTemp = new Bne(cond.tempLeft, cond.tempRight, branchLabel);
			}
			if(cond.getType().type == Scope.InnerType.FLOAT) {
				tempRegister = generateTemp(Scope.InnerType.INT);
				floatTemp = new Feq(cond.tempLeft, cond.tempRight, tempRegister);
				branchTemp = new Beq(tempRegister, "x0", branchLabel);
			}
			break;
			case EQ:
			//Reversed operation for branch instruction NE
			//if(left != right)
			if(cond.getType().type == Scope.InnerType.INT || cond.getType().type == Scope.InnerType.PTR) {
				branchTemp = new Beq(cond.tempLeft, cond.tempRight, branchLabel);
			}
			if(cond.getType().type == Scope.InnerType.FLOAT) {
				tempRegister = generateTemp(Scope.InnerType.INT);
				floatTemp = new Feq(cond.tempLeft, cond.tempRight, tempRegister);
				branchTemp = new Bne(tempRegister, "x0", branchLabel);
			}
			break;
			default: throw new Error("IfStatementNode is not working properly.");
		}
		
		//Step 4/5:
		Instruction jump = new J(outputLabel);
		Instruction elseL = new Label(elseLabel);
		Instruction outputL = new Label(outputLabel);

		if(floatTemp != null) {
			co.code.add(floatTemp);
		}
		// branchLabel = outputlabel if elist == null, branchLabel = elselabel otherwise
		co.code.add(branchTemp);		//<flipped branch> branchLabel
		co.code.addAll(tlist.code);		//<then code>
		
		if (elist != null) {
			co.code.add(jump);			//j outLabel
			co.code.add(elseL);			//elseLabel:
			co.code.addAll(elist.code); //<else code>
		}
		co.code.add(outputL);			//outLabel:
		
		co.lval = false;
		co.type = cond.getType();

		return co;
	}
		/**
	 * FILL IN FROM STEP 3
	 * 
	 * Step 0: Create code object
	 * 
	 * Step 1: generate labels
	 * 
	 * Step 2: add code from conditional expression
	 * 
	 * Step 3: create branch statement (if not created as part of step 2)
	 * 			don't forget to generate correct branch based on type
	 * 
	 * Step 4: generate code
	 * 		loopLabel:
	 *		<cond code>
	 *		<flipped branch> outLabel
	 *		<body code>
	 *		j loopLabel
	 *		outLabel:
	 *
	 * Step 5 insert code into code object in appropriate order.
	 */
	@Override
	protected CodeObject postprocess(WhileNode node, CodeObject cond, CodeObject slist) {
		//Step 0:
		CodeObject co = new CodeObject();

		//Step 1:
		String outputLabel = generateOutLabel();
		String loopLabel = generateLoopLabel();
		Instruction loopL = new Label(loopLabel);
		co.code.add(loopL);				//loopLabel:
		
		//Step 2:
		co.code.addAll(cond.code);		//<cond code>  
		
		//Step 3:
		Instruction branchTemp = null;
		Instruction floatTemp = null;
		String tempRegister;
		
		switch(node.getCond().getReversedOp()) {
			case GT:
			//Reversed operation for branch instruction LE
			//if(left <= right)
			if(cond.getType().type == Scope.InnerType.INT || cond.getType().type == Scope.InnerType.PTR) {
				branchTemp = new Bgt(cond.tempLeft, cond.tempRight, outputLabel);
			}
			if(cond.getType().type == Scope.InnerType.FLOAT) {
				tempRegister = generateTemp(Scope.InnerType.INT);
				floatTemp = new Flt(cond.tempRight, cond.tempLeft, tempRegister);
				branchTemp = new Bne(tempRegister, "x0", outputLabel);
			}
			break;
			case GE:
			//Reversed operation for branch instruction LT
			//if(left < right)
			if(cond.getType().type == Scope.InnerType.INT || cond.getType().type == Scope.InnerType.PTR) {
				branchTemp = new Bge(cond.tempLeft, cond.tempRight, outputLabel);
			}
			if(cond.getType().type == Scope.InnerType.FLOAT) {
				tempRegister = generateTemp(Scope.InnerType.INT);
				floatTemp = new Fle(cond.tempRight, cond.tempLeft, tempRegister);
				branchTemp = new Bne(tempRegister, "x0", outputLabel);
			}
			break;
			case LT:
			//Reversed operation for branch instruction GE
			//if(left >= right)
			if(cond.getType().type == Scope.InnerType.INT || cond.getType().type == Scope.InnerType.PTR) {
				branchTemp = new Blt(cond.tempLeft, cond.tempRight, outputLabel);
			}
			if(cond.getType().type == Scope.InnerType.FLOAT) {
				tempRegister = generateTemp(Scope.InnerType.INT);
				floatTemp = new Flt(cond.tempLeft, cond.tempRight, tempRegister);
				branchTemp = new Bne(tempRegister, "x0", outputLabel);
			}
			break;
			case LE:
			//Reversed operation for branch instruction GT
			//if(left > right)
			if(cond.getType().type == Scope.InnerType.INT || cond.getType().type == Scope.InnerType.PTR) {
				branchTemp = new Ble(cond.tempLeft, cond.tempRight, outputLabel);
			}
			if(cond.getType().type == Scope.InnerType.FLOAT) {
				tempRegister = generateTemp(Scope.InnerType.INT);
				floatTemp = new Fle(cond.tempLeft, cond.tempRight, tempRegister);
				branchTemp = new Bne(tempRegister, "x0", outputLabel);
			}
			break;
			case NE:
			//Reversed operation for branch instruction EQ
			//if(left == right)
			if(cond.getType().type == Scope.InnerType.INT || cond.getType().type == Scope.InnerType.PTR) {
				branchTemp = new Bne(cond.tempLeft, cond.tempRight, outputLabel);
			}
			if(cond.getType().type == Scope.InnerType.FLOAT) {
				tempRegister = generateTemp(Scope.InnerType.INT);
				floatTemp = new Feq(cond.tempLeft, cond.tempRight, tempRegister);
				branchTemp = new Beq(tempRegister, "x0", outputLabel);
			}
			break;
			case EQ:
			//Reversed operation for branch instruction NE
			//if(left != right)
			if(cond.getType().type == Scope.InnerType.INT || cond.getType().type == Scope.InnerType.PTR) {
				branchTemp = new Beq(cond.tempLeft, cond.tempRight, outputLabel);
			}
			if(cond.getType().type == Scope.InnerType.FLOAT) {
				tempRegister = generateTemp(Scope.InnerType.INT);
				floatTemp = new Feq(cond.tempLeft, cond.tempRight, tempRegister);
				branchTemp = new Bne(tempRegister, "x0", outputLabel);
			}
			break;
			default: throw new Error("WhileNode is not working properly.");
		}
		
		//Step 4/5:
		Instruction jump = new J(loopLabel);
		Instruction outputL = new Label(outputLabel);
		
		if(floatTemp != null) {
			co.code.add(floatTemp);
		}
		co.code.add(branchTemp);		//<flipped branch> outLabel			
		co.code.addAll(slist.code);		//<body code>
		co.code.add(jump);				//j loopLabel
		co.code.add(outputL);			//outLabel:

		co.lval = false;
		co.type = cond.getType();

		return co;
	}

	/**
	 * FILL IN FOR STEP 4
	 * 
	 * Generating code for returns
	 * 
	 * Step 0: Generate new code object
	 * 
	 * Step 1: Add retExpr code to code object (rvalify if necessary)
	 * 
	 * Step 2: Store result of retExpr in appropriate place on stack (fp + 8)
	 * 
	 * Step 3: Jump to out label (use @link{generateFunctionOutLabel()})
	 */
	@Override
	protected CodeObject postprocess(ReturnNode node, CodeObject retExpr) {
		CodeObject co = new CodeObject();
		
		if (retExpr != null) {
			//Step 1 
			if (retExpr.lval == true) {
				retExpr = rvalify(retExpr);
			}
			co.code.addAll(retExpr.code);		

			//Step 2
			if (retExpr.getType().type == (Scope.InnerType.INT) || retExpr.getType().type == (Scope.InnerType.PTR)) {
				co.code.add(new Sw(retExpr.temp, "fp", String.valueOf(8)));
			}	
			if (retExpr.getType().type == (Scope.InnerType.FLOAT)) {
				co.code.add(new Fsw(retExpr.temp, "fp", String.valueOf(8)));	
			}

			//Step 3
			co.code.add(new J(generateFunctionOutLabel()));
			co.lval = false;
			co.type = retExpr.getType();
		}

		return co;
	}

	@Override
	protected void preprocess(FunctionNode node) {
		// Generate function label information, used for other labels inside function
		currFunc = node.getFuncName();

		//reset register counts; each function uses new registers!
		intRegCount = 0;
		floatRegCount = 0;
	}

	/**
	 * FILL IN FOR STEP 4
	 * 
	 * Generate code for functions
	 * 
	 * Step 1: add the label for the beginning of the function
	 * 
	 * Step 2: manage frame  pointer
	 * 			a. Save old frame pointer
	 * 			b. Move frame pointer to point to base of activation record (current sp)
	 * 			c. Update stack pointer
	 * 
	 * Step 3: allocate new stack frame (use scope infromation from FunctionNode)
	 * 
	 * Step 4: save registers on stack (Can inspect intRegCount and floatRegCount to know what to save)
	 * 
	 * Step 5: add the code from the function body
	 * 
	 * Step 6: add post-processing code:
	 * 			a. Label for `return` statements inside function body to jump to
	 * 			b. Restore registers
	 * 			c. Deallocate stack frame (set stack pointer to frame pointer)
	 * 			d. Reset fp to old location
	 * 			e. Return from function
	 */
	@Override
	protected CodeObject postprocess(FunctionNode node, CodeObject body) {
		CodeObject co = new CodeObject();

		//Step 1
		co.code.add(new Label(generateFunctionLabel()));				

		//Step 2
		//a
		co.code.add(new Sw("fp", "sp", "0"));
		//b
		co.code.add(new Mv("sp", "fp"));		
		//c	
		co.code.add(new Addi("sp", String.valueOf(-4), "sp"));

		//Step 3 
		co.code.add(new Addi("sp", String.valueOf(node.getScope().getNumLocals() * -4), "sp"));
		
		//Step 4 	
		for (int i = 1; i <= getIntRegCount(); i++) { 
			co.code.add(new Sw("t".concat(String.valueOf(i)), "sp", "0"));		
			co.code.add(new Addi("sp", String.valueOf(-4), "sp"));																										
		}
		for (int f = 1; f <= getFloatRegCount(); f++) {														
			co.code.add(new Fsw("f".concat(String.valueOf(f)), "sp", "0")); 	
			co.code.add(new Addi("sp", String.valueOf(-4), "sp"));
		}
		
		//Step 5
		co.code.addAll(body.code);

		//Step 6
		//a	 
		co.code.add(new Label(generateFunctionOutLabel()));								
		//b
		for (int f = getFloatRegCount(); f > 0; f--) {												
			co.code.add(new Addi("sp", String.valueOf(4), "sp"));
			co.code.add(new Flw("f".concat(String.valueOf(f)), "sp", "0")); 	
		}
		for (int i = getIntRegCount(); i > 0; i--) {
			co.code.add(new Addi("sp", String.valueOf(4), "sp"));
			co.code.add(new Lw("t".concat(String.valueOf(i)), "sp", "0"));											
		}		
		//c 
		co.code.add(new Mv("fp", "sp"));									
		//d
		co.code.add(new Lw("fp", "fp", "0"));	
		//e
		co.code.add(new Ret());

		return co;
	}

	/**
	 * Generate code for the list of functions. This is the "top level" code generation function
	 * 
	 * Step 1: Set fp to point to sp
	 * 
	 * Step 2: Insert a JR to main
	 * 
	 * Step 3: Insert a HALT
	 * 
	 * Step 4: Include all the code of the functions
	 */
	@Override
	protected CodeObject postprocess(FunctionListNode node, List<CodeObject> funcs) {
		CodeObject co = new CodeObject();

		co.code.add(new Mv("sp", "fp"));
		co.code.add(new Jr(generateFunctionLabel("main")));
		co.code.add(new Halt());
		co.code.add(new Blank());

		//add code for each of the functions
		for (CodeObject c : funcs) {
			co.code.addAll(c.code);
			co.code.add(new Blank());
		}

		return co;
	}

	/**
	* 
	* FILL IN FOR STEP 4
	* 
	* Generate code for a call expression
	 * 
	 * Step 1: For each argument:
	 * 
	 * 	Step 1a: insert code of argument (don't forget to rvalify!)
	 * 
	 * 	Step 1b: push result of argument onto stack 
	 * 
	 * Step 2: alloate space for return value
	 * 
	 * Step 3: push current return address onto stack
	 * 
	 * Step 4: jump to function
	 * 
	 * Step 5: pop return address back from stack
	 * 
	 * Step 6: pop return value into fresh temporary (destination of call expression)
	 * 
	 * Step 7: remove arguments from stack (move sp)
	 * 
	 * Add special handling for malloc and free
	 */

	 /**
	  * FOR STEP 6: Make sure to handle VOID functions properly
	  */
	@Override
	protected CodeObject postprocess(CallNode node, List<CodeObject> args) {
		
		//STEP 0
		CodeObject co = new CodeObject();

		//STEP 1
		Instruction pushI = new Addi("sp", String.valueOf(-4), "sp");
		for	(CodeObject c : args) {
			//1a
			if (c.lval == true) {			
				c = rvalify(c);	
			}
			co.code.addAll(c.code);
			//1b
			if (c.getType().type.equals(Scope.InnerType.INT) || c.getType().type.equals((Scope.InnerType.PTR))) {
				co.code.add(new Sw(c.temp, "sp", "0")); 
				co.code.add(pushI);		
			}	
			if (c.getType().type.equals(Scope.InnerType.FLOAT)) {
				co.code.add(new Fsw(c.temp, "sp", "0"));	
				co.code.add(pushI);		
			}
		}		
		
		//Step 2
		co.code.add(pushI);
		
		//Step 3			
		co.code.add(new Sw("ra", "sp", "0"));
		co.code.add(pushI);
		
		//Step 4
		co.code.add(new Jr(generateFunctionLabel(node.getFuncName())));
		
		//Step 5
		co.code.add(new Addi("sp", String.valueOf(4), "sp"));
		co.code.add(new Lw("ra", "sp", "0"));
		co.code.add(new Addi("sp", String.valueOf(4), "sp"));
	
		if (node.getType().type != Scope.InnerType.VOID) {
			//Step 6
			Instruction popI = null;
			if (node.getType().type == Scope.InnerType.PTR) {
				popI = new Lw(generateTemp(Scope.InnerType.PTR), "sp", String.valueOf(0));
			}
			if (node.getType().type == Scope.InnerType.INT) {											
				popI = new Lw(generateTemp(Scope.InnerType.INT), "sp", String.valueOf(0));			
			} 
			if (node.getType().type == Scope.InnerType.FLOAT) {
				popI = new Flw(generateTemp(Scope.InnerType.FLOAT), "sp", String.valueOf(0));
			}

			//Step 7
			co.code.add(popI);
			co.temp = popI.getDest();
		}
		
		co.code.add(new Addi("sp", String.valueOf(4 * (args.size())), "sp"));
		co.type = node.getType();
		co.lval = false;
		
		return co;
	}	
	
	/**
	 * Generate code for * (expr)
	 * 
	 * Goal: convert the r-val coming from expr (a computed address) into an l-val (an address that can be loaded/stored)
	 * 
	 * Step 0: Create new code object
	 * 
	 * Step 1: Rvalify expr if needed
	 * 
	 * Step 2: Copy code from expr (including any rvalification) into new code object
	 * 
	 * Step 3: New code object has same temporary as old code, but now is marked as an l-val
	 * 
	 * Step 4: New code object has an "unwrapped" type: if type of expr is * T, type of temporary is T. Can get this from node
	 */
	@Override
	protected CodeObject postprocess(PtrDerefNode node, CodeObject expr) {
		CodeObject co = new CodeObject();
		
		//Step 1
		if (expr.lval == true){
			expr = rvalify(expr);
		}

		//Step 2
		co.code.addAll(expr.code);
		
		//Step 3
		co.temp = expr.temp;
		co.lval = true;
	
		//Step 4
		co.type = expr.getType().getWrappedType();

		return co;
	}

	/**
	 * Generate code for a & (expr)
	 * 
	 * Goal: convert the lval coming from expr (an address) to an r-val (a piece of data that can be used)
	 * 
	 * Step 0: Create new code object
	 * 
	 * Step 1: If lval is a variable, generate code to put address into a register (e.g., generateAddressFromVar)
	 *			Otherwise just copy code from other code object
	 * 
	 * Step 2: New code object has same temporary as existing code, but is an r-val
	 * 
	 * Step 3: New code object has a "wrapped" type. If type of expr is T, type of temporary is *T. Can get this from node
	 */
	@Override
	protected CodeObject postprocess(AddrOfNode node, CodeObject expr) {
		CodeObject co = new CodeObject();

		// Step 1
		if (expr.isVar()) {
			expr.code.addAll(generateAddrFromVariable(expr));	
			expr.temp = expr.code.getLast().getDest();
		}	
		co.code.addAll(expr.code);

		// Step 2
		co.temp = expr.temp;
		co.lval = false;

		// Step 3
		co.type = Scope.Type.pointerToType(expr.getType());

		return co;
	}

	/**
	 * Generate code for malloc
	 * 
	 * Step 0: Create new code object
	 * 
	 * Step 1: Add code from expression (rvalify if needed)
	 * 
	 * Step 2: Create new MALLOC instruction
	 * 
	 * Step 3: Set code object type to INFER
	 */
	@Override
	protected CodeObject postprocess(MallocNode node, CodeObject expr) {
		CodeObject co = new CodeObject();

		// Step 1
		if (expr.lval == true) {
			expr = rvalify(expr);
		}
		co.code.addAll(expr.code);
		
		// Step 2
		Instruction mallocIns = null;
		if (expr.getType().type == Scope.InnerType.PTR) {
			String temp = generateTemp(Scope.InnerType.PTR);
			mallocIns = new Malloc(expr.temp, temp);
			co.temp = temp;
		}
		if (expr.getType().type == Scope.InnerType.INT) {											
			String temp = generateTemp(Scope.InnerType.INT);
			mallocIns = new Malloc(expr.temp, temp);
			co.temp = temp;		
		} 
		if (expr.getType().type == Scope.InnerType.FLOAT) {
			String temp = generateTemp(Scope.InnerType.FLOAT);
			mallocIns = new Malloc(expr.temp, temp);
			co.temp = temp;
		}
		co.code.add(mallocIns);

		// Step 3
		co.type = new Scope.Type(Scope.InnerType.INFER);
		co.lval = false;									
		
		return co;
	}
	
	/**
	 * Generate code for free
	 * 
	 * Step 0: Create new code object
	 * 
	 * Step 1: Add code from expression (rvalify if needed)
	 * 
	 * Step 2: Create new FREE instruction
	 */
	@Override
	protected CodeObject postprocess(FreeNode node, CodeObject expr) {
		CodeObject co = new CodeObject();

		// Step 1 
		if (expr.lval == true){
			expr = rvalify(expr);
		}
		co.code.addAll(expr.code);

		// Step 2
		Instruction freeIns = new Free(expr.temp);
		co.code.add(freeIns);
		co.type = expr.getType();
		co.lval = false;
		co.temp = expr.temp;

		return co;
	}

	/**
	 * Generate code for cast
	 * 
	 *
	 * 
	 *
	 * 
	 * 
	 */
	@Override
	protected CodeObject postprocess(CastExprNode node, CodeObject expr) {
		CodeObject co = new CodeObject();
		
		// Step 1::Rvalify
		if (expr.lval == true){
			expr = rvalify(expr);
		}
		co.code.addAll(expr.code);
		
		//node.getCastType(); 	// THIS IS THE TYPE YOU WANT TO CAST TO
		//expr.getType();		// THIS IS THE CURRENT TYPE

		// Step 2::Convert	
		Instruction hardConvert = null;
		/* If you cast float to int
		float x;
		(int) x;
		*/
		if (expr.getType().type == Scope.InnerType.FLOAT && node.getCastType().type == Scope.InnerType.INT){
			hardConvert = new Fmovis(expr.temp, generateTemp(Scope.InnerType.INT));
			co.code.add(hardConvert);
		}/* If you cast int to float
		int x;
		(float) x;
		*/
		else if(expr.getType().type == Scope.InnerType.FLOAT && node.getCastType().type == Scope.InnerType.INT){
			hardConvert = new Imovfs(expr.temp, generateTemp(Scope.InnerType.FLOAT));
			co.code.add(hardConvert);
		}
		
		co.type = node.getCastType();
		co.lval = false;
		co.temp = co.code.getLast().getDest();

		return co;
	}

	/**
	 * Generate a fresh temporary
	 * 
	 * @return new temporary register name
	 */
	protected String generateTemp(Scope.InnerType t) {
		switch(t) {
			case INT: 
			case PTR: //works the same for pointers
				return intTempPrefix + String.valueOf(++intRegCount);
			case FLOAT: return floatTempPrefix + String.valueOf(++floatRegCount);
			default: throw new Error("Generating temp for bad type");
		}
	}

	protected String generateLoopLabel() {
		return "loop_" + String.valueOf(++loopLabel);
	}

	protected String generateElseLabel() {
		return  "else_" + String.valueOf(++elseLabel);
	}

	protected String generateOutLabel() {
		return "out_" +  String.valueOf(++outLabel);
	}

	protected String generateFunctionLabel() {
		return "func_" + currFunc;
	}

	protected String generateFunctionLabel(String func) {
		return "func_" + func;
	}

	protected String generateFunctionOutLabel() {
		return "func_ret_" + currFunc;
	}
	
	/**
	 * Take a code object that results in an lval, and create a new code
	 * object that adds a load to generate the rval.
	 * 
	 * @param lco The code object resulting in an address
	 * @return A code object with all the code of <code>lco</code> followed by a load
	 *         to generate an rval
	 */
	protected CodeObject rvalify(CodeObject lco) {
		
		assert (lco.lval == true);
		// Step 0
		CodeObject co = new CodeObject();
		SymbolTableEntry symbol = lco.getSTE();
		Scope.Type lcoType = lco.getType();

		// Step 1
		if (lco.isVar() == true) {
			lco.code.addAll(generateAddrFromVariable(lco));	
			lco.temp = lco.code.getLast().getDest();	
		}
		co.code.addAll(lco.code);
		//Step 2
		Instruction loadTemp = null;
		if (lcoType.type == Scope.InnerType.INT || lco.getType().type == Scope.InnerType.PTR) {
			if(symbol != null) {
				if(symbol.isLocal()) {
					loadTemp = new Lw(generateTemp(lcoType.type), "fp", symbol.addressToString());
				}
			}
			else {
				loadTemp = new Lw(generateTemp(lcoType.type), lco.temp, "0");
			}
		}
		if (lcoType.type == Scope.InnerType.FLOAT) {
			if(symbol != null) {
				if(symbol.isLocal()) {
					loadTemp = new Flw(generateTemp(lcoType.type), "fp", symbol.addressToString());
				}
			}
			else {
				loadTemp = new Flw(generateTemp(lcoType.type), lco.temp, "0");
			}
		}
			
		co.code.add(loadTemp);
		co.lval = false;
		co.temp = loadTemp.getDest();
		co.type = lcoType;
		/* FILL IN FROM STEP 2 */

		/* DON'T FORGET TO ADD CODE TO GENERATE LOADS FOR LOCAL VARIABLES */

		return co;
	}

	/**
	 * Generate an instruction sequence that holds the address of the variable in a code object
	 * 
	 * If it's a global variable, just get the address from the symbol table
	 * 
	 * If it's a local variable, compute the address relative to the frame pointer (fp)
	 * 
	 * @param lco The code object holding a variable
	 * @return a list of instructions that puts the address of the variable in a register
	 */
	private InstructionList generateAddrFromVariable(CodeObject lco) {

		InstructionList il = new InstructionList();

		//Step 1:
		SymbolTableEntry symbol = lco.getSTE();
		String address = symbol.addressToString();

		//Step 2:
		Instruction compAddr = null;
		if (symbol.isLocal()) {
			//If local, address is offset
			//need to load fp + offset
			//addi tmp' fp offset
			compAddr = new Addi("fp", address, generateTemp(Scope.InnerType.INT));
		} else {
			//If global, address in symbol table is the right location
			//la tmp' addr //Register type needs to be an int
			compAddr = new La(generateTemp(Scope.InnerType.INT), address);
		}
		il.add(compAddr); //add instruction to code object

		return il;
	}

}