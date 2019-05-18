# tech.python

Techascent bindings to the python ecosystem.

## Installation

```console
chrisn@chrisn-dt:~/dev/cnuernber/libpython-clj$ sudo update-alternatives --config java
[sudo] password for chrisn:
There are 2 choices for the alternative java (providing /usr/bin/java).

  Selection    Path                                            Priority   Status
  ------------------------------------------------------------
    0            /usr/lib/jvm/java-11-openjdk-amd64/bin/java      1111      auto mode
    1            /usr/lib/jvm/java-11-openjdk-amd64/bin/java      1111      manual mode
  * 2            /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java   1081      manual mode

  Press <enter> to keep the current choice[*], or type selection number: ^C
chrisn@chrisn-dt:~/dev/cnuernber/libpython-clj$ JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/ pip3 install jep --user
```


## Usage

```clojure

;;Create a thread-local interpreter
user> (import '[jep Jep])
jep.Jep
user> (def myjep (Jep.))
#'user/myjep

;;Eval some code.  The only way to read data out is to assign results to variables.
user> (.eval myjep "t = [object(), 1, 1.5, True, None, [], (), {'key':'value'} ]")
true
user> (import '[jep.python PyObject])
jep.python.PyObject
user> (.getValue myjep "t", (Class/forName "[Ljep.python.PyObject;"))
[#object[jep.python.PyObject 0x503a2bed "<object object at 0x7fb85bac4150>"],
 #object[jep.python.PyObject 0x447ae3e4 "1"],
 #object[jep.python.PyObject 0x1eb0fd33 "1.5"],
 #object[jep.python.PyObject 0xd9e632a "True"], nil,
 #object[jep.python.PyObject 0x58c4bd1d "[]"],
 #object[jep.python.PyObject 0x4b321022 "()"],
 #object[jep.python.PyObject 0x52723512 "{'key': 'value'}"]]



;;Lets get into some numpy.  Jep has support for this through their
;;ndarray objects.  Tech has support via a protocols.  NDArrays are first
;;class citizens if you require the right namespace: [tech.libs.jep.ndarray]

user> (.eval myjep "import numpy as np")
true
user> (.eval myjep "t = np.ones((2,3))")
true
user> (.getValue myjep "t")
#object[jep.NDArray 0x5bcc9658 "jep.NDArray@2de134a2"]
user> (require '[tech.libs.jep.ndarray])
:tech.resource.gc Reference thread starting
user> (require '[tech.v2.datatype :as dtype])
nil
user> (def test-np-ary (.getValue myjep "t"))
#'user/test-np-ary
user> (dtype/shape test-np-ary)
[2 3]
user> (require '[tech.v2.tensor :as dtt])
nil

;; ND arrays have zero-copy conversion to tensors.
user> (println (dtt/ensure-tensor test-np-ary))
#tech.v2.tensor<float64>[2 3]
[[1.000 1.000 1.000]
 [1.000 1.000 1.000]]
nil

;; But let's say you want to create your own tensor outside of this context.
user> (def test-tensor (dtt/->tensor (partition 3 (range 9))))
#'user/test-tensor
user> (require '[tech.libs.jep.protocols :as jep-proto])
nil
user> (jep-proto/as-nd-array test-tensor)
#object[jep.NDArray 0x348e8765 "jep.NDArray@34c4baa3"]
user> (def test-tensor (dtt/->tensor (partition 3 (range 9))
                                     :container-type :native-buffer))
19-05-17 23:48:51 chrisn-lt-2 INFO [tech.jna.timbre-log:8] - Library c found at [:system "c"]


;; If you create a native-backed tensor then it will be a directndarray
user> (jep-proto/as-nd-array test-tensor)
#object[jep.DirectNDArray 0x1f904cd8 "jep.DirectNDArray@c3d4179f"]
user> (.set myjep "t" (jep-proto/as-nd-array test-tensor))
nil
user> (.eval myjep "t = t + 2")
true


;;The results do not mirror back which means t was reallocated.
user> (println test-tensor)
#tech.v2.tensor<float64>[3 3]
[[0.000 1.000 2.000]
 [3.000 4.000 5.000]
 [6.000 7.000 8.000]]
nil


;;That being said, the values did transfer

user> (.eval myjep "b = str(t)")
true
user> (.getValue myjep "b")
"[[ 2.  3.  4.]\n [ 5.  6.  7.]\n [ 8.  9. 10.]]"


;;Reset t as the original test tensor object.
user> (.set myjep "t" (jep-proto/as-nd-array test-tensor))

;; Modifications do in fact work the other direction though
user> (require '[tech.v2.datatype.functional :as dfn])
nil
;;We change the underlying values of the test tensor buffer via realizing
;;a lazy reader chain with copy.
user> (dtype/copy! (dfn/+ test-tensor 4) test-tensor)
...
user> (println test-tensor)
#tech.v2.tensor<float64>[3 3]
[[ 4.000  5.000  6.000]
 [ 7.000  8.000  9.000]
 [10.000 11.000 12.000]]
nil
user> (.eval myjep "b = str(t)")
true

;; Python does in fact reflect the values.
user> (.getValue myjep "b")
"[[ 4.  5.  6.]\n [ 7.  8.  9.]\n [10. 11. 12.]]"
user>
```


## License

Copyright © 2019 TechAscent, LLC

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.
