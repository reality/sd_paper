library(readr)
library(fmsb)


i1 <- c(0.01, 0.02, 0.05)
i2 <- c(5155, 4948, 4305)

facet_counts <- read_delim("data/create_facet_counts/facet_counts.tsv", 
                           delim = "\t", escape_double = FALSE, 
                           trim_ws = TRUE)

facet_counts <- facet_counts[facet_counts$facet != "phenotypic abnormality",]

# comparitive 
facet_counts_f <- data.frame(t(facet_counts[2:3]))
colnames(facet_counts_f) <- c(1,2,3)
colnames(facet_counts_f) <- unlist(facet_counts[,1])
facet_counts_f <- rbind(rep(0.15,20) , rep(0,20) , facet_counts_f)
colors_border=c( rgb(0.2,0.5,0.5,0.9), rgb(0.8,0.2,0.5,0.9) , rgb(0.7,0.5,0.1,0.9) )
colors_in=c( rgb(0.2,0.5,0.5,0.4), rgb(0.8,0.2,0.5,0.4) , rgb(0.7,0.5,0.1,0.4) )

#png("smdpvsbldp.png", units="in", width=8, height=7, res=300)
png("smdpvsbldp.png", units="in", width=7, height=6, res=300)
par(mar = c(0,0,0,0))
  radarchart(facet_counts_f,
           pcol=colors_border , pfcol=colors_in , plwd=4 , plty=1,
           cglcol="grey", cglty=1, axislabcol="grey", axistype=1, 
           caxislabels = c('0%', '5%', '10%', '15%', '20%'),
           #caxislabels=seq(0,0.2,.05), 
           cglwd=.8,
           vlcex=.9,
           )
legend(x=.9, y=1, legend = c("SM-DP", "BDL-DP"), bty = "n", pch=20 , col=colors_in , text.col = "grey", cex=1, pt.cex=2)
dev.off()

#novel 
facet_counts_f <- data.frame(t(facet_counts[4]))
colnames(facet_counts_f) <- c(1,2,3)
colnames(facet_counts_f) <- unlist(facet_counts[,1])
facet_counts_f <- rbind(rep(0.15,20) , rep(0,20) , facet_counts_f)
colors_border=c( rgb(0.2,0.5,0.5,0.9), rgb(0.8,0.2,0.5,0.9) , rgb(0.7,0.5,0.1,0.9) )
colors_in=c( rgb(0.2,0.5,0.5,0.4), rgb(0.8,0.2,0.5,0.4) , rgb(0.7,0.5,0.1,0.4) )

png("smdp_novel.png", units="in", width=7, height=6, res=300)
par(mar = c(0,0,0,0))
radarchart(facet_counts_f,
           pcol=colors_border , pfcol=colors_in , plwd=4 , plty=1,
           cglcol="grey", cglty=1, axislabcol="grey", axistype=1,
           caxislabels = c('0%', '5%', '10%', '15%', '20%'),cglwd=0.8,
           vlcex=.9
)
dev.off()
