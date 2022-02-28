#!/bin/bash
dir="/home/symmaries/Symmaries/Examples/RadioCRM";
variant="_ImplicitConf_-exceptions_deepalias_200_-dr_timeout=2m_cuddmem=4GiB_";
syrsvers="$( syrs.opt -V )";
if test -r "${dir}/results${variant}.syrs-version"; then oldvers="$(< "${dir}/results${variant}.syrs-version")"; else oldvers="none"; fi;
if test -r "${dir}/types.syrs-version"; then oldtypvers="$(< "${dir}/types.syrs-version")"; else oldtypvers="none"; fi;
if test ! "${oldtypvers}" = "${syrsvers}" || test ! -f "${dir}/types.classes_bin" -o "${dir}/types.classes" -nt "${dir}/types.classes_bin"; then \
  OCAMLRUNPARAM="h=3G" /usr/bin/time -f '%e; %S; %U; %M; %x' -o "${dir}/types.proc_stats" syrs.opt -ll w -tf -exceptions,-taints,levels=all,heapdom=deepalias "${dir}/types.classes" -o "${dir}/types.classes_bin" -o "${dir}/types.class_stats" && \
  echo "${syrsvers}" > "${dir}/types.syrs-version"; \
fi && \
if test ! "${oldvers}" = "${syrsvers}"; then \
  OCAMLRUNPARAM="h=3G" /usr/bin/time -f '%e; %S; %U; %M; %x' -o "${dir}/results${variant}_analysis.proc_stats" syrs.opt -ll w -tf -exceptions,-taints,levels=all,heapdom=deepalias "${dir}/types.classes_bin" "${dir}/Meth/all.secstubs" "${dir}/Meth/all.meth_files" -of secsum -rf -dr -rf cuddmem=4GiB -pf timeout=2m --methskip-cond 200 --safe-walk -o "${dir}/results${variant}.secsums" -o "${dir}/results${variant}.meth_stats" && \
  echo "${syrsvers}" > "${dir}/results${variant}.syrs-version" && \
  for f in ${dir}/Meth/*.secsum; do \
     mv "$f" "${f%.secsum}.${variant}-secsum"; \
  done; \
fi


