library(readr)
library(fmsb)


i1 <- c(0.01, 0.02, 0.05)
i2 <- c(5155, 4948, 4305)

 facet_counts <- read_delim("data/DOID:289_facet_counts.tsv", 
                           delim = "\t", escape_double = FALSE, 
                           trim_ws = TRUE)

facet_counts$`log_Social Media` <- facet_counts$`Social Media` * log(facet_counts$`Social Media`)
facet_counts$log_Literature <- facet_counts$Literature * log(facet_counts$Literature)

facet_counts$`log_Social Media`[is.nan(facet_counts$`log_Social Media`)] <- 0
facet_counts$log_Literature[is.nan(facet_counts$log_Literature)] <- 0

#entropy
-sum(facet_counts$`log_Social Media`)
-sum(facet_counts$`log_Literature`)

facet_counts$log_Literature <- NULL
facet_counts$`log_Social Media`<- NULL

facet_counts <- facet_counts[facet_counts$facet != "phenotypic abnormality",]
facet_counts_f <- data.frame(t(facet_counts[-1]))
colnames(facet_counts_f) <- c(1,2,3)
colnames(facet_counts_f) <- unlist(facet_counts[,1])
facet_counts_f <- rbind(rep(0.55,20) , rep(0,20) , facet_counts_f)


colors_border=c( rgb(0.2,0.5,0.5,0.9), rgb(0.8,0.2,0.5,0.9) , rgb(0.7,0.5,0.1,0.9) )
colors_in=c( rgb(0.2,0.5,0.5,0.4), rgb(0.8,0.2,0.5,0.4) , rgb(0.7,0.5,0.1,0.4) )


  radarchart(facet_counts_f,
           pcol=colors_border , pfcol=colors_in , plwd=4 , plty=1,
           cglcol="grey", cglty=1, axislabcol="grey", caxislabels=seq(0,20,5), cglwd=0.8,
           vlcex=0.8,
           )
legend(x=1, y=1, legend = rownames(facet_counts_f[-c(1,2),]), bty = "n", pch=20 , col=colors_in , text.col = "grey", cex=1, pt.cex=3)
#mtext(side = 3, line = 1, at = 0, cex = 0.75, "Distribution of phenotype categories for endometriosis phenotypes derived from social media vs literature text", 
#      col = '#666664')
