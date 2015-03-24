/*******************************************************************************
 * Copyright (c) 2009-2015 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
 *   * Tijs van der Storm - Tijs.van.der.Storm@cwi.nl
 *   * Paul Klint - Paul.Klint@cwi.nl - CWI
 *   * Mark Hills - Mark.Hills@cwi.nl (CWI)
 *   * Arnold Lankamp - Arnold.Lankamp@cwi.nl
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *******************************************************************************/
package org.rascalmpl.ast;


import org.eclipse.imp.pdb.facts.IConstructor;

public abstract class LocationLiteral extends AbstractAST {
  public LocationLiteral(IConstructor node) {
    super();
  }

  
  public boolean hasPathPart() {
    return false;
  }

  public org.rascalmpl.ast.PathPart getPathPart() {
    throw new UnsupportedOperationException();
  }
  public boolean hasProtocolPart() {
    return false;
  }

  public org.rascalmpl.ast.ProtocolPart getProtocolPart() {
    throw new UnsupportedOperationException();
  }

  

  
  public boolean isDefault() {
    return false;
  }

  static public class Default extends LocationLiteral {
    // Production: sig("Default",[arg("org.rascalmpl.ast.ProtocolPart","protocolPart"),arg("org.rascalmpl.ast.PathPart","pathPart")])
  
    
    private final org.rascalmpl.ast.ProtocolPart protocolPart;
    private final org.rascalmpl.ast.PathPart pathPart;
  
    public Default(IConstructor node , org.rascalmpl.ast.ProtocolPart protocolPart,  org.rascalmpl.ast.PathPart pathPart) {
      super(node);
      
      this.protocolPart = protocolPart;
      this.pathPart = pathPart;
    }
  
    @Override
    public boolean isDefault() { 
      return true; 
    }
  
    @Override
    public <T> T accept(IASTVisitor<T> visitor) {
      return visitor.visitLocationLiteralDefault(this);
    }
  
    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Default)) {
        return false;
      }        
      Default tmp = (Default) o;
      return true && tmp.protocolPart.equals(this.protocolPart) && tmp.pathPart.equals(this.pathPart) ; 
    }
   
    @Override
    public int hashCode() {
      return 337 + 449 * protocolPart.hashCode() + 557 * pathPart.hashCode() ; 
    } 
  
    
    @Override
    public org.rascalmpl.ast.ProtocolPart getProtocolPart() {
      return this.protocolPart;
    }
  
    @Override
    public boolean hasProtocolPart() {
      return true;
    }
    @Override
    public org.rascalmpl.ast.PathPart getPathPart() {
      return this.pathPart;
    }
  
    @Override
    public boolean hasPathPart() {
      return true;
    }	
  }
}