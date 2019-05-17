(ns tech.libs.jep.ndarray
  (:require [tech.v2.datatype.protocols :as dtype-proto]
            [tech.v2.datatype :as dtype]
            [tech.v2.datatype.base :as dtype-base]
            [tech.v2.datatype.typed-buffer :as typed-buffer]
            [tech.v2.datatype.casting :as casting]
            [tech.libs.jep.protocols :as jep-proto])
  (:import [jep DirectNDArray NDArray AbstractNDArray]
           [java.lang.reflect Field]))

(set! *warn-on-reflection* true)

(def data-field
  (let [field (-> AbstractNDArray
                  (.getDeclaredField "data"))]
    (.setAccessible field true)
    field))


(defn- internal-get-data
  [item]
  (.get ^Field data-field item))


(defprotocol PNDBindings
  (is-unsigned [item])
  (dimensions [item]))


(extend-protocol PNDBindings
  NDArray
  (is-unsigned? [item] (.isUnsigned item))
  (dimensions [item] (.getDimensions item))
  DirectNDArray
  (is-unsigned? [item] (.isUnsigned item))
  (dimensions [item] (.getDimensions item)))


(defn to-typed-buffer
  [^AbstractNDArray ary]
  (-> (internal-get-data ary)
      (typed-buffer/set-datatype (dtype/get-datatype ary))))



(extend-type AbstractNDArray
  dtype-proto/PDatatype
  (get-datatype [item]
    (let [item-data (internal-get-data item)
          base-datatype (dtype/get-datatype item-data)]
      (if (is-unsigned? item)
        (get casting/signed-unsigned base-datatype base-datatype)
        base-datatype)))
  dtype-proto/PCountable
  (ecount [item] (apply * (dtype-proto/shape item)))
  dtype-proto/PShape
  (shape [item] (vec (dimensions item)))


  dtype-proto/PCopyRawData
  (copy-raw->item! [raw-data target offset options]
    (dtype-base/raw-dtype-copy! raw-data target offset options))

  dtype-proto/PToNioBuffer
  (convertible-to-nio-buffer? [item] true)
  (->buffer-backing-store [item]
    (dtype-proto/->buffer-backing-store (internal-get-data item)))

  dtype-proto/PToList
  (convertible-to-fastutil-list [item]
    (dtype-proto/convertible-to-fastutil-list?
     (to-typed-buffer item)))
  (->list-backing-store [item]
    (dtype-proto/->list-backing-store
     (to-typed-buffer item)))

  dtype-proto/PToArray
  (->sub-array [item]
    (dtype-proto/->sub-array (to-typed-buffer item)))
  (->array-copy [item]
    (dtype-proto/->array-copy (to-typed-buffer item)))

  dtype-proto/PPrototype
  (from-prototype [item datatype shape]
    (let [new-buf (dtype/make-container :native-buffer datatype
                                        (dtype-base/shape->ecount shape))]
      (DirectNDArray. (dtype-proto/as-nio-buffer new-buf)
                      (boolean (casting/unsigned-integer-type? datatype))
                      (int-array shape))))

  dtype-proto/PSetConstant
  (set-constant! [item offset value elem-count]
    (dtype-proto/set-constant! (to-typed-buffer item) offset value elem-count))


  dtype-proto/PWriteIndexes
  (write-indexes! [item indexes values options]
    (dtype-proto/write-indexes! (to-typed-buffer item) indexes values options))

  dtype-proto/PReadIndexes
  (read-indexes! [item indexes values options]
    (dtype-proto/read-indexes! (to-typed-buffer item) indexes values options))

  dtype-proto/PToReader
  (convertible-to-reader? [item] true)
  (->reader [item options]
    (dtype-proto/->reader (to-typed-buffer item) options))

  dtype-proto/PToWriter
  (convertible-to-writer? [item] true)
  (->writer [item options]
    (dtype-proto/->writer (to-typed-buffer item) options))


  dtype-proto/PToIterable
  (convertible-to-iterable? [item] true)
  (->iterable [item options]
    (dtype-proto/->reader item options)))



(defmethod dtype-proto/make-container :jep-ndarray
  [container-type datatype elem-count-or-seq options]
  (when-not (casting/numeric-type? datatype)
    (throw (ex-info "Cannot create non-numeric jep ndarrays"
                    {:datatype datatype})))
  (-> (dtype/make-container :native-buffer datatype elem-count-or-seq options)
      (jep-proto/as-nd-array)))
