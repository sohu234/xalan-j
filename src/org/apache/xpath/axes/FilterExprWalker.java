/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 1999-2003 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:  
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Xalan" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written 
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 1999, Lotus
 * Development Corporation., http://www.lotus.com.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.xpath.axes;

import org.apache.xml.dtm.Axis;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xpath.Expression;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.VariableComposeState;
import org.apache.xpath.XPathContext;
import org.apache.xpath.XPathVisitor;
import org.apache.xpath.functions.Function;
import org.apache.xpath.objects.XNodeSet;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.objects.XSequence;
import org.apache.xpath.objects.XSequenceSingleton;
import org.apache.xpath.parser.Node;
import org.apache.xpath.parser.PathExpr;

/**
 * Walker for the OP_VARIABLE, or OP_EXTFUNCTION, or OP_FUNCTION, or OP_GROUP,
 * op codes.
 * @see <a href="http://www.w3.org/TR/xpath#NT-FilterExpr">XPath FilterExpr descriptions</a>
 */
public class FilterExprWalker extends AxesWalker
{

  /**
   * Construct a FilterExprWalker using a LocPathIterator.
   *
   * @param locPathIterator non-null reference to the parent iterator.
   */
  public FilterExprWalker(WalkingIterator locPathIterator)
  {
    super(locPathIterator, Axis.FILTEREDLIST);
  }

  /**
   * Init a FilterExprWalker.
   *
   * @param stepExpr The FilterExpr's expression.
   *
   * @throws javax.xml.transform.TransformerException
   */
  public void init(org.apache.xpath.parser.StepExpr stepExpr)
    throws javax.xml.transform.TransformerException
  {
    int childCount = stepExpr.jjtGetNumChildren();
    if (childCount > 0)
    {
      Node child = stepExpr.jjtGetChild(0);
      m_expr = (Expression) child;
      // Not sure if this is still needed.  Probably not. -sb
      if (m_expr instanceof WalkingIterator)
      {
        WalkingIterator wi = (WalkingIterator) m_expr;
        if (wi.getFirstWalker() instanceof FilterExprWalker)
        {
          FilterExprWalker fw = (FilterExprWalker) wi.getFirstWalker();
          if (null == fw.getNextWalker())
          {
            m_expr = fw.m_expr;
            m_expr.exprSetParent(this);
          }
        }

      }
      if (m_expr instanceof Function)
      {
        m_mustHardReset = true;
      }
      if (m_expr instanceof org.apache.xalan.templates.FuncKey)
      {
        // hack/temp workaround
        m_canDetachNodeset = false;
      }
      m_expr.exprSetParent(this);

    }
    super.init(stepExpr);

    // Smooth over an anomily in the opcode map...
    //    switch (stepType)
    //    {
    //    case OpCodes.OP_FUNCTION :
    //    case OpCodes.OP_EXTFUNCTION :
    //    	m_mustHardReset = true;
    //    case OpCodes.OP_GROUP :
    //    case OpCodes.OP_VARIABLE :
    //      m_expr = compiler.compile(opPos);
    //      m_expr.exprSetParent(this);
    //      if((OpCodes.OP_FUNCTION == stepType) && (m_expr instanceof org.apache.xalan.templates.FuncKey))
    //      {
    //      	// hack/temp workaround
    //      	m_canDetachNodeset = false;
    //      }
    //      break;
    //    default :
    //      m_expr = compiler.compile(opPos + 2);
    //      m_expr.exprSetParent(this);
    //    }
    //    if(m_expr instanceof WalkingIterator)
    //    {
    //      WalkingIterator wi = (WalkingIterator)m_expr;
    //      if(wi.getFirstWalker() instanceof FilterExprWalker)
    //      {
    //      	FilterExprWalker fw = (FilterExprWalker)wi.getFirstWalker();
    //      	if(null == fw.getNextWalker())
    //      	{
    //      		m_expr = fw.m_expr;
    //      		m_expr.exprSetParent(this);
    //      	}
    //      }
    //      		
    //    }
  }

  /**
   * Detaches the walker from the set which it iterated over, releasing
   * any computational resources and placing the iterator in the INVALID
   * state.
   */
  public void detach()
  {
    super.detach();
    m_exprObj.detach();
    m_exprObj = null;
  }

  /**
   *  Set the root node of the TreeWalker.
   *
   * @param root non-null reference to the root, or starting point of 
   *        the query.
   */
  public void setRoot(int root)
  {

    super.setRoot(root);

    m_exprObj =
      FilterExprIteratorSimple.executeFilterExpr(
        root,
        m_lpi.getXPathContext(),
        m_lpi.getPrefixResolver(),
        m_lpi.getIsTopLevel(),
        m_lpi.m_stackFrame,
        m_expr);

  }

  /**
   * Get a cloned FilterExprWalker.
   *
   * @return A new FilterExprWalker that can be used without mutating this one.
   *
   * @throws CloneNotSupportedException
   */
  public Object clone() throws CloneNotSupportedException
  {

    FilterExprWalker clone = (FilterExprWalker) super.clone();

    // clone.m_expr = (Expression)((Expression)m_expr).clone();
    if (null != m_exprObj)
      clone.m_exprObj = (XNodeSet) m_exprObj.clone();

    return clone;
  }

  /**
   * This method needs to override AxesWalker.acceptNode because FilterExprWalkers
   * don't need to, and shouldn't, do a node test.
   * @param n  The node to check to see if it passes the filter or not.
   * @return  a constant to determine whether the node is accepted,
   *   rejected, or skipped, as defined  above .
   */
  public short acceptNode(int n)
  {

    try
    {
      if (getPredicateCount() > 0)
      {
        countProximityPosition(0);

        if (!executePredicates(n, m_lpi.getXPathContext()))
          return DTMIterator.FILTER_SKIP;
      }

      return DTMIterator.FILTER_ACCEPT;
    }
    catch (javax.xml.transform.TransformerException se)
    {
      throw new RuntimeException(se.getMessage());
    }
  }

  /**
   *  Moves the <code>TreeWalker</code> to the next visible node in document
   * order relative to the current node, and returns the new node. If the
   * current node has no next node,  or if the search for nextNode attempts
   * to step upward from the TreeWalker's root node, returns
   * <code>null</code> , and retains the current node.
   * @return  The new node, or <code>null</code> if the current node has no
   *   next node  in the TreeWalker's logical view.
   */
  public int getNextNode()
  {

    if (null != m_exprObj)
    {
      int next;
      XObject item = m_exprObj.next();
      if(item != null && item != XSequence.EMPTY)
        next = item.getNodeHandle();
      else
      {
        m_exprObj.reset();
        next = DTM.NULL;
      }
      return next;
    }
    else
      return DTM.NULL;
  }

  /**
   * Get the index of the last node that can be itterated to.
   *
   *
   * @param xctxt XPath runtime context.
   *
   * @return the index of the last node that can be itterated to.
   */
  public int getLastPos(XPathContext xctxt)
  {
    return m_exprObj.getLength();
  }

  /** The contained expression. Should be non-null.
   *  @serial   */
  private Expression m_expr;

  /** The result of executing m_expr.  Needs to be deep cloned on clone op.  */
  transient private XSequence m_exprObj;

  private boolean m_mustHardReset = false;
  private boolean m_canDetachNodeset = true;

  /**
   * This function is used to fixup variables from QNames to stack frame 
   * indexes at stylesheet build time.
   * @param vars List of QNames that correspond to variables.  This list 
   * should be searched backwards for the first qualified name that 
   * corresponds to the variable reference qname.  The position of the 
   * QName in the vector from the start of the vector will be its position 
   * in the stack frame (but variables above the globalsTop value will need 
   * to be offset to the current stack frame).
   */
  public void fixupVariables(VariableComposeState vcs)
  {
    super.fixupVariables(vcs);
    m_expr.fixupVariables(vcs);
  }

  /**
   * Get the inner contained expression of this filter.
   */
  public Expression getInnerExpression()
  {
    return m_expr;
  }

  /**
   * Set the inner contained expression of this filter.
   */
  public void setInnerExpression(Expression expr)
  {
    expr.exprSetParent(this);
    m_expr = expr;
  }

  /** 
   * Get the analysis bits for this walker, as defined in the WalkerFactory.
   * @return One of WalkerFactory#BIT_DESCENDANT, etc.
   */
  public int getAnalysisBits()
  {
    if (null != m_expr && m_expr instanceof PathComponent)
    {
      return ((PathComponent) m_expr).getAnalysisBits();
    }
    return WalkerFactory.BIT_FILTER;
  }

  /**
   * Returns true if all the nodes in the iteration well be returned in document 
   * order.
   * Warning: This can only be called after setRoot has been called!
   * 
   * @return true as a default.
   */
  public boolean isDocOrdered()
  {
    if(m_exprObj instanceof XNodeSet)
      return ((XNodeSet)m_exprObj).isDocOrdered();
    else if(m_exprObj instanceof XSequenceSingleton)
      return true;
    else
      return false;
  }

  /**
   * Returns the axis being iterated, if it is known.
   * 
   * @return Axis.CHILD, etc., or -1 if the axis is not known or is of multiple 
   * types.
   */
  public int getAxis()
  {
    if(null != m_exprObj && m_exprObj instanceof XNodeSet)
      return ((XNodeSet)m_exprObj).getAxis();
    else
      return Axis.FILTEREDLIST;
  }

  class filterExprOwner implements ExpressionOwner
  {
    /**
    * @see ExpressionOwner#getExpression()
    */
    public Expression getExpression()
    {
      return m_expr;
    }

    /**
     * @see ExpressionOwner#setExpression(Expression)
     */
    public void setExpression(Expression exp)
    {
      exp.exprSetParent(FilterExprWalker.this);
      m_expr = exp;
    }

  }

  /**
   * This will traverse the heararchy, calling the visitor for 
   * each member.  If the called visitor method returns 
   * false, the subtree should not be called.
   * 
   * @param owner The owner of the visitor, where that path may be 
   *              rewritten if needed.
   * @param visitor The visitor whose appropriate method will be called.
   */
  public void callPredicateVisitors(XPathVisitor visitor)
  {
    m_expr.callVisitors(new filterExprOwner(), visitor);

    super.callPredicateVisitors(visitor);
  }

  /**
   * @see Expression#deepEquals(Expression)
   */
  public boolean deepEquals(Expression expr)
  {
    if (!super.deepEquals(expr))
      return false;

    FilterExprWalker walker = (FilterExprWalker) expr;
    if (!m_expr.deepEquals(walker.m_expr))
      return false;

    return true;
  }

  public void jjtAddChild(Node n, int i)
  {
    if (n instanceof AxesWalker)
    {
      super.jjtAddChild(n, i);
    }
    else // Do we care about i?
      {
      n = fixupPrimarys(n);
      m_expr = (Expression) n;
    }

  }

  public Node jjtGetChild(int i)
  {
    if (i == 0)
      return m_expr;
    else
      if ((null != m_nextWalker) && i == 1)
        return m_nextWalker;
      else
        return null;
  }

  public int jjtGetNumChildren()
  {
    return ((null == m_nextWalker) ? 0 : 1) + ((null == m_expr) ? 0 : 1);
  }

  /**
   * This function checks the integrity of the tree, after it has been fully built and 
   * is ready for execution.  Derived classes can overload this function to check 
   * their own assumptions.
   */
  public boolean checkTreeIntegrity(
    int levelCount,
    int childNumber,
    boolean isOK)
  {
    if (null == m_expr)
      isOK =
        flagProblem(
          toString()
            + " the expression for FilterExpr can not be null at this point!");
    return super.checkTreeIntegrity(levelCount, childNumber, isOK);
  }
  
  /**
   * @see org.apache.xpath.parser.SimpleNode#isPathExpr()
   */
  public boolean isPathExpr()
  {
    // keep it from reducing if not a nodeset expression.
    return (m_expr instanceof DTMIterator) ? true : 
      (m_expr instanceof PathExpr) ? true : false; 
  }

}
