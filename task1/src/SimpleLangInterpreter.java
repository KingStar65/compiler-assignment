import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

public class SimpleLangInterpreter extends AbstractParseTreeVisitor<Integer> implements SimpleLangVisitor<Integer> {

    private final Map<String, SimpleLangParser.DecContext> global_funcs = new HashMap<>();
    private final Stack<Map<String, Integer>> frames = new Stack<>();

    public Integer visitProgram(SimpleLangParser.ProgContext ctx, String[] args)
    {
        for (int i = 0; i < ctx.dec().size(); ++i) {
            SimpleLangParser.DecContext dec = ctx.dec(i);
            SimpleLangParser.Typed_idfrContext typedIdfr = dec.typed_idfr(0);
            global_funcs.put(typedIdfr.Idfr().getText(), dec);

        }

        SimpleLangParser.DecContext main = global_funcs.get("main");

        Map<String, Integer> newFrame = new HashMap<>(); //there is an error in this chunk

            for (int i = 0; i < main.vardec.size() && i < args.length; ++i) {
                SimpleLangParser.Typed_idfrContext param = main.vardec.get(i);
                String paramName = param.Idfr().getText();
                newFrame.put(paramName, Integer.parseInt(args[i]));
            }

        frames.push(newFrame);
        return visit(main);


    }

    @Override public Integer visitProg(SimpleLangParser.ProgContext ctx)
    {
        throw new RuntimeException("Should not be here!");

    }

    @Override public Integer visitDec(SimpleLangParser.DecContext ctx)
    {
        //System.out.println("Dec");
        Integer returnValue = visit(ctx.body());
        frames.pop();
        return returnValue;

    }


    @Override public Integer visitTyped_idfr(SimpleLangParser.Typed_idfrContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }

    @Override public Integer visitType(SimpleLangParser.TypeContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }

    @Override public Integer visitBody(SimpleLangParser.BodyContext ctx) {

        Integer returnValue = null;
        for (int i = 0; i < ctx.ene.size(); ++i) {
            SimpleLangParser.ExpContext exp = ctx.ene.get(i);
            returnValue = visit(exp);
        }
        return returnValue;

    }

    @Override public Integer visitBlock(SimpleLangParser.BlockContext ctx)
    {
        //System.out.println("visitBlock");
        Integer returnValue = null;
        for (int i = 0; i < ctx.ene.size(); ++i) {
            SimpleLangParser.ExpContext exp = ctx.ene.get(i);
            returnValue = visit(exp);
        }
        return returnValue;
    }

    @Override
    public Integer visitVarDeclExpr(SimpleLangParser.VarDeclExprContext ctx) {

        String varName = ctx.typed_idfr().Idfr().getText();
        Integer value = visit(ctx.exp());
        frames.peek().put(varName, value);
        return value;
    }

    @Override public Integer visitAssignExpr(SimpleLangParser.AssignExprContext ctx)
    {

        SimpleLangParser.ExpContext rhs = ctx.exp();
        frames.peek().replace(ctx.Idfr().getText(), visit(rhs));
        return null;

    }

    @Override public Integer visitBinOpExpr(SimpleLangParser.BinOpExprContext ctx) {

        SimpleLangParser.ExpContext operand1 = ctx.exp(0);
        Integer oprnd1 = visit(operand1);
        SimpleLangParser.ExpContext operand2 = ctx.exp(1);
        Integer oprnd2 = visit(operand2);

        switch (((TerminalNode) (ctx.binop().getChild(0))).getSymbol().getType()) {

            case SimpleLangParser.Eq ->  {
                return ((Objects.equals(oprnd1, oprnd2)) ? 1 : 0);

            }
            case SimpleLangParser.Less -> {

                return ((oprnd1 < oprnd2) ? 1 : 0);

            }
            case SimpleLangParser.LessEq -> {

                return ((oprnd1 <= oprnd2) ? 1 : 0);

            }
            case SimpleLangParser.Plus -> {

                return oprnd1 + oprnd2;

            }
            case SimpleLangParser.Minus -> {

                return oprnd1 - oprnd2;

            }
            case SimpleLangParser.Times -> {

                return oprnd1 * oprnd2;

            }
            case SimpleLangParser.More -> {
                return (oprnd1 > oprnd2) ? 1 : 0;
            }
            case SimpleLangParser.MoreEq -> {

                return ((oprnd1 >= oprnd2) ? 1 : 0);

            }
            case SimpleLangParser.Divide -> {
                return oprnd1 / oprnd2;
            }
            case SimpleLangParser.And -> {
                return (oprnd1 != 0 && oprnd2 != 0) ? 1 : 0;
            }
            default -> {
                throw new RuntimeException("Shouldn't be here - wrong binary operator.");
            }

        }

    }
    @Override public Integer visitInvokeExpr(SimpleLangParser.InvokeExprContext ctx)
    {

        SimpleLangParser.DecContext dec = global_funcs.get(ctx.Idfr().getText());
        Map<String, Integer> newFrame = new HashMap<>();

        for (int i = 0; i < ctx.args.size(); i++) {
            SimpleLangParser.ExpContext exp = ctx.args.get(i);
            SimpleLangParser.Typed_idfrContext param = dec.vardec.get(i);
            newFrame.put(param.Idfr().getText(), visit(exp));
        }

        frames.push(newFrame);
        return visit(dec);

    }

    @Override public Integer visitBlockExpr(SimpleLangParser.BlockExprContext ctx) {
        return visit(ctx.block());
    }

    @Override public Integer visitIfExpr(SimpleLangParser.IfExprContext ctx) {
        //System.out.println("ifExpr1");
        SimpleLangParser.ExpContext cond = ctx.exp();
        Integer condValue = visit(cond);
        //System.out.println("Condition Value: " + condValue);

        if (condValue > 0) {
            //System.out.println("then block");
            SimpleLangParser.BlockContext thenBlock = ctx.block(0);
            return visit(thenBlock);
        } else {
            //System.out.println("else block");
            SimpleLangParser.BlockContext elseBlock = ctx.block(1);
            return visit(elseBlock);
        }
    }

    @Override
    public Integer visitWhileExpr(SimpleLangParser.WhileExprContext ctx) {
        SimpleLangParser.ExpContext cond = ctx.exp();
        Integer condValue = visit(cond);
        Integer theValue = 0;
        while (condValue > 0) {
            SimpleLangParser.BlockContext block = ctx.block();
            theValue = visit(block);
            condValue = visit(cond);
        }

        return theValue;
    }

    @Override
    public Integer visitRepeatExpr(SimpleLangParser.RepeatExprContext ctx) {
        Integer returnValue = visit(ctx.block());

        // Evaluate the condition after executing the block
        Integer condValue = visit(ctx.exp());

        // Continue executing the block until the condition is false
        while (condValue == 0) {
            returnValue = visit(ctx.block());
            condValue = visit(ctx.exp());
        }

        return returnValue;
    }

    @Override public Integer visitPrintExpr(SimpleLangParser.PrintExprContext ctx) {

        SimpleLangParser.ExpContext exp = ctx.exp();
        if (exp instanceof SimpleLangParser.SpaceExprContext) {
            System.out.print(" ");
        } else if (exp instanceof SimpleLangParser.NewLineExprContext) {
            System.out.println();
        } else {
            System.out.print(visit(exp));
        }
        return null;

    }

    @Override public Integer visitSpaceExpr(SimpleLangParser.SpaceExprContext ctx) {
        return null;
    }

    @Override public Integer visitIdExpr(SimpleLangParser.IdExprContext ctx)
    {
        return frames.peek().get(ctx.Idfr().getText());
    }

    @Override public Integer visitIntExpr(SimpleLangParser.IntExprContext ctx)
    {
        //System.out.println("IntExpr");
        //return 0
       return Integer.parseInt(ctx.IntLit().getText());


    }

    @Override
    public Integer visitBoolExpr(SimpleLangParser.BoolExprContext ctx) {
        String boolLit = ctx.BoolLit().getText();
        return boolLit.equals("true") ? 1 : 0;
    }


    @Override
    public Integer visitNewLineExpr(SimpleLangParser.NewLineExprContext ctx) {
        return null;
    }

    @Override
    public Integer visitSkipExpr(SimpleLangParser.SkipExprContext ctx) {
        return null;
    }


    @Override public Integer visitEqBinop(SimpleLangParser.EqBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }
    @Override public Integer visitLessBinop(SimpleLangParser.LessBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }
    @Override public Integer visitLessEqBinop(SimpleLangParser.LessEqBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }

    @Override
    public Integer visitMoreBinop(SimpleLangParser.MoreBinopContext ctx) {
        return null;
    }

    @Override
    public Integer visitMoreEqBinop(SimpleLangParser.MoreEqBinopContext ctx) {
        return null;
    }

    @Override public Integer visitPlusBinop(SimpleLangParser.PlusBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }
    @Override public Integer visitMinusBinop(SimpleLangParser.MinusBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }
    @Override public Integer visitTimesBinop(SimpleLangParser.TimesBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }

    @Override
    public Integer visitDivideBinop(SimpleLangParser.DivideBinopContext ctx) {
        return null;
    }

    @Override
    public Integer visitAndBinop(SimpleLangParser.AndBinopContext ctx) {
        return null;
    }

}
