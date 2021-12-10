package ast;

import ast.visitor.ASTVisitor;
import compiler.Scope;

/**
 * A node for cast expressions 
 * 
 * This has one child: the {@link ExpressionNode} being operated on
 */
public class CastExprNode extends ExpressionNode {
	
	private ExpressionNode expr;
	private Scope.Type castType;
	// STEP 7
	public CastExprNode(ExpressionNode expr, Scope.Type type) {
		this.castType(type);
		this.setExpr(expr);
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor) {
		return visitor.visit(this);
	}

	public ASTNode getExpr() {
		return expr;
	}

	private void setExpr(ExpressionNode right) {
		this.expr = right;
	}
	// STEP 7 edit
	public Scope.Type getCastType() {
		return castType;
	}
	// STEP 7 edit
	private void castType(Scope.Type type) {
		this.castType = type;
		//this.expr.setType(type);  // Didn't need this
	}
}