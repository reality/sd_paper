if (!require("BiocManager", quietly = TRUE))
  install.packages("BiocManager")

if (!require("qvalue", quietly=T))
  BiocManager::install("qvalue")

library(qvalue)
library(readr)

q <- read_delim("data/process_associations/associations.tsv", 
                           delim = "\t", escape_double = FALSE, 
                           trim_ws = TRUE, col_names = F)
 
qobj <- qvalue(q$X4, fdr.level=0.0005) 
summary(qobj$significant)
q$q = qobj$qvalues
q$sig = qobj$significant

write_tsv(q, 'data/qval/associations.tsv')

assoc <- read.delim("~/smdp/data/norm.tsv", header=FALSE)
library(ggpubr)
ggqqplot(assoc$V1)
hist(assoc$V1, breaks=8, freq=F)
