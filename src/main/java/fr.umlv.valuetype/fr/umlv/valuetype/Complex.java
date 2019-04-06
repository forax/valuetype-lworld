package fr.umlv.valuetype;

public value class Complex {
  private final double re;
  private final double im;

  public Complex(double re, double im) {
    this.re = re;
    this.im = im;
  }

  public double squareDistance() {
    return re * re + im * im;
  }

  public Complex square() {
    return new Complex(re * re - im * im, 2 * re * im);
  }

  public Complex add(Complex c) {
    return new Complex(re + c.re, im + c.im);
  }
}