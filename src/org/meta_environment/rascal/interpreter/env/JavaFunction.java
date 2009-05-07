package org.meta_environment.rascal.interpreter.env;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.meta_environment.rascal.ast.FunctionDeclaration;
import org.meta_environment.rascal.interpreter.Evaluator;
import org.meta_environment.rascal.interpreter.EvaluatorContext;
import org.meta_environment.rascal.interpreter.JavaBridge;
import org.meta_environment.rascal.interpreter.Names;
import org.meta_environment.rascal.interpreter.asserts.ImplementationError;
import org.meta_environment.rascal.interpreter.control_exceptions.Throw;
import org.meta_environment.rascal.interpreter.result.Result;
import org.meta_environment.rascal.interpreter.result.ResultFactory;


public class JavaFunction extends Lambda {
	private final Method method;
	private FunctionDeclaration func;
	
	@SuppressWarnings("unchecked")
	public JavaFunction(Evaluator eval, FunctionDeclaration func, boolean varargs, Environment env, JavaBridge javaBridge) {
		super(func, eval,
				TE.eval(func.getSignature().getType(),env),
				Names.name(func.getSignature().getName()), 
				TE.eval(func.getSignature().getParameters(), env),
				varargs, Collections.EMPTY_LIST, env);
		this.method = javaBridge.compileJavaMethod(func, env);
		this.func = func;
	}
	
	@Override
	public Result<IValue> call(IValue[] actuals, Type actualTypes, Environment env) {
		if (hasVarArgs) {
			actuals = computeVarArgsActuals(actuals, formals);
		}
		
		if (callTracing) {
			printStartTrace();
		}

		try {
			IValue result = invoke(actuals);

			if (hasVarArgs) {
				actualTypes = computeVarArgsActualTypes(actualTypes, formals);
			}

			bindTypeParameters(actualTypes, formals, env); 
			Type resultType = returnType.instantiate(env.getStore(), env.getTypeBindings());
			return ResultFactory.makeResult(resultType, result, new EvaluatorContext(eval, ast));
		}
		finally {
			if (callTracing) {
				printEndTrace();
			}
		}
	}
	
	public IValue invoke(IValue[] actuals) {
		try {
			return (IValue) method.invoke(null, (Object[]) actuals);
		} catch (SecurityException e) {
			throw new ImplementationError("Unexpected security exception", e);
		} catch (IllegalArgumentException e) {
			throw new ImplementationError("An illegal argument was generated for a generated method", e);
		} catch (IllegalAccessException e) {
			throw new ImplementationError("Unexpected illegal access exception", e);
		} catch (InvocationTargetException e) {
			Throwable targetException = e.getTargetException();
			
			if (targetException instanceof Throw) {
				Throw th = (Throw) targetException;
				((Throw) targetException).setLocation(eval.getCurrentAST().getLocation());
				((Throw) targetException).setTrace(eval.getStackTrace());
				throw th;
			}
			
			throw org.meta_environment.rascal.interpreter.RuntimeExceptionFactory.javaException(targetException.getMessage(), eval.getCurrentAST(), eval.getStackTrace());
		}
	}
	
	@Override
	public String toString() {
		return func.toString();
	}
	
}
