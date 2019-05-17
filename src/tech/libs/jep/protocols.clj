(ns tech.libs.jep.protocols
  (:require [tech.v2.datatype.protocols :as dtype-proto]
            [tech.v2.datatype.casting :as casting]
            [tech.v2.datatype :as dtype]
            [tech.jna :as jna])
  (:import [jep DirectNDArray NDArray]))


(set! *warn-on-reflection* true)


(defprotocol PToNDArray
  (convertible-to-nd-array? [item])
  ;;return value must share backing store with item.
  (->nd-array [item]))


(extend-type Object
  PToNDArray
  (convertible-to-nd-array? [item]
    (dtype-proto/convertible-to-nio-buffer? item))
  ;;return value must share backing store with item.
  (->nd-array [item]
    (when-let [item-buffer (dtype/as-nio-buffer item)]
      (let [item-shape (int-array (dtype/shape item))
            unsigned? (boolean (casting/unsigned-integer-type? (dtype/get-datatype item)))]
        (if (jna/as-ptr item-buffer)
          (DirectNDArray. item-buffer unsigned? item-shape)
          (when-let [ary-data (dtype/->sub-array item-buffer)]
            (let [{:keys [java-array length offset]} ary-data]
              (when (and (= 0 (long offset))
                         (= (long length) (dtype/ecount java-array)))
                (NDArray. java-array unsigned? item-shape)))))))))


(defn as-nd-array
  "Convert to a backing-store-sharing nd-array"
  [item]
  (when (convertible-to-nd-array? item)
    (->nd-array item)))


(defn copy-to-nd-array
  [item]
  (when-not (casting/numeric-type? (dtype/get-datatype item))
    (throw (ex-info "Only numeric datatypes can be nd-arrays." {})))
  (let [elem-count (dtype/ecount item)
        datatype (dtype/get-datatype item)
        unsigned? (casting/unsigned-integer-type?
                   (dtype/get-datatype item))
        backing-store (dtype/copy!
                       item
                       (dtype/make-container :native-buffer
                                             datatype
                                             elem-count))]
    (DirectNDArray. (dtype/as-nio-buffer backing-store)
                    (boolean unsigned?)
                    (int-array (dtype/shape item)))))


(defn to-nd-array
  [item]
  (if-let [retval (->nd-array item)]
    retval
    (copy-to-nd-array item)))
