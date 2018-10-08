package com.github.spotbugs;

public class Bar implements Cloneable { //causes CN:CN_IDIOM

  public static void main(String[] args) {
    System.out.println("hello!");
  }

}
