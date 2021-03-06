;; Copyright (c) 2014 Jamison Hope
;; This is free software;  for terms and warranty disclaimer see ./COPYING.

(module-name (kawa quaternions))
(export quaternion quaternion?
        real-part imag-part jmag-part kmag-part complex-part
        vector-part unit-quaternion unit-vector
        vector-quaternion? make-vector-quaternion
        vector-quaternion->list
        magnitude angle colatitude longitude
        make-rectangular make-polar
        + - * /
        dot-product cross-product conjugate
        exp log expt sqrt
        sin cos tan asin acos atan)

(import (kawa base)
        (only kawa.lib.numbers
              quaternion quaternion? unit-vector
              real-part imag-part jmag-part kmag-part))

(define (complex-part x::java.lang.Number) ::complex
  (if (gnu.math.Quaternion? x)
      ((->gnu.math.Quaternion x):complexPart)
      x))

(define (vector-part x::java.lang.Number) ::quaternion
  (if (gnu.math.Quaternion? x)
      ((->gnu.math.Quaternion x):vectorPart)
      0))

(define (colatitude x::java.lang.Number) ::java.lang.Number
  (if (gnu.math.Quaternion? x)
      ((->gnu.math.Quaternion x):colatitude)
      0))

(define (longitude x::java.lang.Number) ::java.lang.Number
  (if (gnu.math.Quaternion? x)
      ((->gnu.math.Quaternion x):longitude)
      0))

(define (unit-quaternion x::java.lang.Number) ::java.lang.Number
  (cond ((gnu.math.Quaternion? x)
         ((->gnu.math.Quaternion x):unitQuaternion))
        ((gnu.math.Quantity? x)
         (quantity:make (unit-quaternion ((->quantity x):number))
                        ((->quantity x):unit)))
        ((zero? x) 0)
        ((negative? x) -1)
        ((positive? x) 1)
        (else +nan.0)))

(define (vector-quaternion? o) ::boolean
  (and (quaternion? o) (zero? (real-part o))))

(define (make-vector-quaternion x::real y::real z::real) ::quaternion
  (make-rectangular 0 x y z))

(define (vector-quaternion->list vec::quaternion) ::list
  (list (imag-part vec) (jmag-part vec) (kmag-part vec)))

(define (dot-product x::java.lang.Number y::java.lang.Number) ::real
  (if (not (and (vector-quaternion? x) (vector-quaternion? y)))
      (error 'dot-product "arguments must be vector quaternions")
      ;; inline expansion of (- (real-part (* x y)))
      (+ (* (imag-part x) (imag-part y))
         (* (jmag-part x) (jmag-part y))
         (* (kmag-part x) (kmag-part y)))))

(define (cross-product x::java.lang.Number y::java.lang.Number) ::quaternion
  (if (not (and (vector-quaternion? x) (vector-quaternion? y)))
      (error 'cross-product "arguments must be vector quaternions")
      (vector-part (* x y))))

(define (conjugate x::java.lang.Number) ::java.lang.Number
  (if (gnu.math.Quaternion? x)
      ((->gnu.math.Quaternion x):conjugate)
      x))
