(module-name (kawa process-keywords))

(require <kawa.lib.prim_imports>)
(require <kawa.lib.std_syntax>)

(define-syntax process-keywords
  (syntax-rules ()
		((process-keywords obj args handle-keyword handle-non-keyword)
		 (let ((num-args :: <int> args:length))
		   (let loop ((i :: <int> 0))
		     (if (< i num-args)
			 (let ((arg (args i)))
			   (cond ((instance? arg <gnu.expr.Keyword>)
				  (handle-keyword obj
						  (gnu.expr.Keyword:getName arg)
						  (args (+ i 1)))
				  (loop (+ i 2)))
				 ((instance? arg <gnu.kawa.xml.KAttr>)
				  (let* ((attr :: <gnu.kawa.xml.KAttr> arg)
					 (name :: <java.lang.String> (invoke attr 'getName))
					 (value (invoke attr 'getObjectValue)))
				    (handle-keyword obj name value))
				  (loop (+ i 1)))
				 (else
				  (handle-non-keyword obj arg)
				  (loop (+ i 1)))))))))))
