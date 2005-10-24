(test-begin "numbers" 1664)

(test-approximate 1.4 (sqrt 2) 0.02)
(test-error
   #t (test-read-eval-string "0.0.0"))
(test-assert
 (eq? 3 3))

;; A problem posed by Ken Dickey (kend@data.UUCP) on comp.lang.lisp
;; to check numerical exactness of Lisp implementations.
(define (dickey-test x y)
  (+  (* 1335/4 (expt y 6))
      (* (expt x 2)
	 (- (* 11 (expt x 2) (expt y 2))
	    (expt y 6)
	    (* 121 (expt y 4))
	    2))
      (* 11/2 (expt y 8))
      (/ x (* 2 y))))
(test-eqv -54767/66192 (dickey-test 77617 33096))

(test-eqv -1/10000000000000 (/ -1 #e1e13))
(test-eqv 9223372036854775808 (- (- (expt 2 63))))
(test-eqv #i1/0 (+ 1e4294967297))
(test-eqv #i1/0 (* 1e429496729742942949672974967297 1))

(test-eqv 500000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
      (quotient (expt 10 200) (* 20 (expt 10 100))))

(test-eqv
 "neg-test" 0 (+ 17280012451545786657095548928 -17280012451545786657095548928))
(test-eqv 1250120440709706990357803482218496
	  (+ 1250137720722158536144460577767424 -17280012451545786657095548928))
(test-eqv 100000000000000
	  (quotient 10000000000000000000000000000000000 100000000000000000000))
(test-eqv 1250120440709706990357803482218496
	  (- 1250137720722158536144460577767424 17280012451545786657095548928))
(test-eqv -1250120440709706990357803482218496
	  (- 17280012451545786657095548928 1250137720722158536144460577767424))

(test-group "expt"
	    (test-eqv 9223372036854775808 (expt 2 63)))

(test-begin "convert")
(test-eqv 10000000000 (inexact->exact (exact->inexact 10000000000)))
(test-eqv 1.4285714285714286e22 (exact->inexact 14285714285714285714285))
(test-eqv 0 (inexact->exact 0.0))
(test-eqv 123451/10 (rationalize (inexact->exact 12345.1) (inexact->exact 0.00001)))
(test-end "convert")

(test-begin "magnitude")
(test-eqv 4.0( magnitude 4.))
(test-eqv 4e3 (magnitude -4000.))
(test-eqv 5.0 (magnitude 3-4i))
(test-eqv 3/2 (magnitude (/ 6 -4)))
(test-end "magnitude")

(test-begin "shift")
(test-eqv 12676506002282294014967032053760 (arithmetic-shift 10 100))
(test-end "shift")

(test-begin "logcount")
(test-eqv 3 (logcount 13))
(test-eqv 2 (logcount -13))
(test-eqv 4 (logcount 30))
(test-eqv 4 (logcount -30))
(test-end "logcount")

(test-begin "gcd")
(test-eqv 3 (gcd 4294967295 3))
(test-eqv 3 (gcd 4294967298 3))
(test-end "gcd")

(test-begin "logop")

;; A Boolean 1-bit version of logop.
(define (logop-bits op x y)
  (odd? (quotient op (* (if x 1 4) (if y 1 2)))))

(define (logop-compare result op x y)
  (do ((i 0 (+ i 1)))
      ((or (= i 100)
	   (not (eq? (logop-bits op (logbit? x i) (logbit? y i))
		     (logbit? result i))))
       i)
    #t))

(define (logop-test1 op x y)
  (logop-compare (logop op x y) op x y))

(define test-vals '(0 1 -1 2 -2 3 -3 #x7fffffff
		      #x-f0f0cccc12345 #x1234567890abcdef0012345))

(define (logop-test op)
  (do ((xl test-vals (cdr xl)))
      ((null? xl) #t)
    (do ((yl test-vals (cdr yl)))
      ((null? yl) #t)
      (test-eqv 100 (logop-test1 op (car xl) (car yl))))))

(do ((i 0 (+ i 1)))
    ((= i 16) #t)
  (logop-test i))
(test-end "logop")

(test-group
 "integer-length"
 (test-eqv 0 (integer-length 0))
 (test-eqv 1 (integer-length 1))
 (test-eqv 2 (integer-length 3))
 (test-eqv 3 (integer-length 4))
 (test-eqv 3 (integer-length 7))
 (test-eqv 0 (integer-length -1))
 (test-eqv 2 (integer-length -4))
 (test-eqv 3 (integer-length -7))
 (test-eqv 3 (integer-length -8))
 (test-eqv 31 (integer-length #x7fffffff))
 (test-eqv 32 (integer-length #xffffffff))
 (test-eqv 33 (integer-length #x100000000)))

(test-eqv 1000000000000000000000000000000 (* 1000000000000000 1000000000000000))

;; From Norman Hardy <norm@netcom.com>
(define (ssin x) (let ((a2 (quotient (* x x) dx)))
   (- x (let tail ((term x)(ndx 2))
      (let ((x (quotient (* a2 term) (* ndx (+ ndx 1) dx))))
         (if (zero? x) 0 (- x (tail x (+ ndx 2)))))))))
(define dx (expt 10 100))
(define pi
  31415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679)
(test-eqv 3 (ssin pi))

(test-eqv #f (= (expt 2. 100) (+ (expt 2 100) 1)))
(test-eqv #t (= (expt 2. 100) (exact->inexact (+ (expt 2 100) 1))))

(test-eqv 2650239300 (remainder 14853098170650239300 4000000000))

(test-equal "8000000000000000"( number->string #x8000000000000000 16))
(test-equal "80000000000000000"( number->string #x80000000000000000 16))
;; From Aubrey Jaffer <agj@alum.mit.edu>
(test-eqv #t (number? (string->number "#i0/0+1i")))
(test-eqv #t (number? (string->number "#i1+0/0i")))
(test-eqv #t (positive? 2147483648))
(test-eqv #t (negative? (string->number "#i-1/0")))

;; From Sven.Hartrumpf@fernuni-hagen.de
(define quotient-fix-1
  (lambda (a b x) (quotient (+ (quotient (* a x 10) b) 5) 10)))
(test-eqv 950 (quotient-fix-1 95 100 1000))
;; Variations on Sven's test:
(define (quotient-fix-2 (a :: <real>))
  (quotient (+ a 20) 10))
(test-eqv 97 (quotient-fix-2 950))
(define (quotient-float (a :: <real>))
  (quotient (+ a 25.0) 10))
(test-eqv 97.0 (quotient-float 950))

(define v (vector 3 -4/5 9 10 (+ 8 1/2) -100))
(java.util.Collections:sort v)
(test-equal "sort-v-1" #(-100 -4/5 3 17/2 9 10) v)
(set! v (vector 1.2 -1.2 8.9 100.0 8.9))
(java.util.Collections:sort v)
(test-equal "sort-v-2" #(-1.2 1.2 8.9 8.9 100.0) v)
(set! v (vector 1 0.5 5/2 8 2.5))
(java.util.Collections:sort v)
(test-equal  "sort-v-3" #(0.5 1 5/2 2.5 8) v)
(set! v #("abc" "aa" "zy" ""))
(java.util.Collections:sort v)
(test-equal "sort-v-4" #("" "aa" "abc" "zy") v)
(set! v #f32(1.2 -1.2 8.9 100.0 8.9))
(java.util.Collections:sort v)
(test-equal "sort-v-5" #f32(-1.2 1.2 8.9 8.9 100.0) v)
(set! v #(#s64(3 5) #s64(3 4 5) #s64(-1) #s64(-5)
	  #s64(-1 20) #s64() #s64(-1 10)))
(java.util.Collections:sort v)
(test-equal
 "sort-v-6"
 #(#s64() #s64(-5) #s64(-1) #s64(-1 10) #s64(-1 20) #s64(3 4 5) #s64(3 5))
 v)
(set! v '("abc" "aa" "zy" ""))
(java.util.Collections:sort v)
(test-assert "sort-v-7" (equal? '("" "aa" "abc" "zy") v))
(set! v #((b 3) (a 1) (b 2) (a 2) (b -1) (a)))
(java.util.Collections:sort v)
(test-equal "sort-v-8" #((a) (a 1) (a 2) (b -1) (b 2) (b 3)) v)
(test-end)
