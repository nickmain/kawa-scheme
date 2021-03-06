(module-name (scheme base))

(require kawa.lib.bytevectors)
(require kawa.lib.case_syntax)
(require kawa.lib.characters)
(require kawa.lib.exceptions)
(require kawa.lib.lists)
(require kawa.lib.misc)
(require kawa.lib.numbers)
(require kawa.lib.parameters)
(require kawa.lib.parameterize)
(require kawa.lib.ports)
(require kawa.lib.strings)
(require kawa.lib.std_syntax)
(require kawa.lib.syntax)
(require kawa.lib.vectors)
(require kawa.lib.misc_syntax) ;; FIXME only for deprecated include
(require kawa.lib.DefineRecordType)
(import (except kawa.lib.prim_imports string))
(import (only kawa.lib.prim_imports (letrec letrec*)))
(import (only kawa.lib.rnrs.unicode string-upcase string-downcase string-foldcase))
(import kawa.mstrings)
(export list->string string-append string-map substring vector->string
        string-downcase string-foldcase string-upcase)
(include "base-exports")
