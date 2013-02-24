(define-variable pp #f)
;; Missing else.
(format #t "loop1: ~s~%"
        (let loop1 ((n 1))
          (cond
           ((= n 0) #t)
           ((and n (if pp #f)) (loop1 (- n 1))))))
;; Diagnostic: bad-voidexp.scm:7:20: warning - missing else where value is required
;; Output: loop1: #t

;; Missing else, condition false
(format #t "loop2: ~s~%"
        (let loop2 ((n 1))
          (cond
           ((= n 0) #t)
           ((and n (if #f #f)) (loop2 (- n 1))))))
;; Diagnostic: bad-voidexp.scm:16:20: warning - missing else where value is required
;; Output: loop2: #t

;; Missing else, condition true
(format #t "loop3: ~s~%"
        (let loop3 ((n 1))
          (cond
           ((= n 0) #t)
           ((and n (if #t #f)) (loop3 (- n 1))))))
;; Output: loop3: #!void

;; void expression in define lhs
(define val4 ((lambda () (sleep 0.015))))
;; Diagnostic: bad-voidexp.scm:29:26: warning - void-valued expression where value is needed

(set! val4 (if (not pp) 5))
;; Diagnostic: bad-voidexp.scm:32:12: warning - missing else where value is required
(format #t "val4 #1: ~w~%" val4)
;; Output: val4 #1: 5

(set! val4 (if (sleep 0.015) 11 12))
(format #t "val4 #2: ~w~%" val4)
;; Diagnostic: bad-voidexp.scm:37:12: warning - void-valued condition is always true
;; Output: val4 #2: 11

;; Based on Savannah bug#18736, "intenal compile error -- svn rev 5816".
;; From Thomas Kirk <tk@research.att.com>
(format #t "test-savannah-18736: ~w~%"
        (let* ((elapsed 0)
               (oldtime (java.lang.System:currentTimeMillis))
               (val ((lambda () (sleep 0.015))))
               (ignored
                (begin
                  (set! elapsed
                        (- (java.lang.System:currentTimeMillis) oldtime))
                  val)))
          ;; While time resolution is non-portable, assume a sleep of 20ms
          ;; will be detectable as taking at least 10ms.
          (>= elapsed 10)))
;; Diagnostic: bad-voidexp.scm:47:33: warning - void-valued expression where value is needed
;; Output: test-savannah-18736: #t

(format #t "compare 3 2: ~s~%"
        (cond ((> 3 2) 'greater)
              ((< 3 2) 'less)))
;; Diagnostic: bad-voidexp.scm:60:9: warning - missing else where value is required
;; Output: compare 3 2: greater

(let ((v (list (case (* 2 3)
                 ((2 3 5 7) 'prime)
                 ((1 4 6 8 9) 'composite)))))
  (format #t "6 is ~s~%" (car v)))
;; Diagnostic: bad-voidexp.scm:65:16: warning - missing else where value is required
;; Output: 6 is composite

;; Savannah bug #27014 "AND vs. VOID"
(begin
  (define (foo) (and (bar) (bar)))
  (define baz #f)
  (define (bar) (set! baz #f)))
;; Diagnostic: bad-voidexp.scm:74:22: warning - void-valued expression where value is needed
