package BalancedParentheses;

import java.util.Stack;

public class Solution {
    public static char [][] tokens  = { { '{','}','{',']'} ,   { '{','}','{',']'}   };

    public static boolean isOPenTerm(){

    };

    public static boolean isBalanced(String expression){
      Stack<Character> stack = new Stack<Character> ();
      for(char c: expression.toCharArray()){
          if (isOPenTerm(c)){
              stack.push(c);
          }else{
              char top = stack.pop();
          }

      }

    }

    public static void main (String[] args){}

}
