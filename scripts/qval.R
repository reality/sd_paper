if (!require("BiocManager", quietly = TRUE))
  install.packages("BiocManager")

BiocManager::install("qvalue")

library(qvalue)
 q <- read_delim("new/data/process_associations/associations.tsv", 
                           delim = "\t", escape_double = FALSE, 
                           trim_ws = TRUE, col_names = F)
 
 qobj <- qvalue(q$X4, fdr.level=0.005) 
 summary(qobj$significant)
q$q = qobj$qvalues
q$sig = qobj$significant

write_tsv(q, 'new/data/qval/associations.tsv')
