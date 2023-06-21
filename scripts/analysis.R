library(readr)
library(fmsb)


i1 <- c(0.01, 0.02, 0.05)
i2 <- c(5155, 4948, 4305)

facet_counts <- read_delim("data/create_facet_counts/facet_counts.tsv", 
                           delim = "\t", escape_double = FALSE, 
                           trim_ws = TRUE)

facet_counts <- facet_counts[facet_counts$facet != "phenotypic abnormality",]
facet_counts$`Social Media` <- as.numeric(facet_counts$`Social Media`)
facet_counts$`Social Media Novel` <- as.numeric(facet_counts$`Social Media Novel`)

# comparitive 
facet_counts_f <- data.frame(t(facet_counts[2:3]))
colnames(facet_counts_f) <- c(1,2,3)
colnames(facet_counts_f) <- unlist(facet_counts[,1])
facet_counts_f <- rbind(rep(8,20) , rep(0,20) , facet_counts_f)

colors_border=c( rgb(0.2,0.5,0.5,0.9), rgb(0.8,0.2,0.5,0.9) , rgb(0.7,0.5,0.1,0.9) )
colors_in=c( rgb(0.2,0.5,0.5,0.4), rgb(0.8,0.2,0.5,0.4) , rgb(0.7,0.5,0.1,0.4) )

#png("smdpvsbldp.png", units="in", width=7, height=6, res=300)
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
#dev.off()

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

facet_counts <- read_delim("data/create_disease_facet_counts/facet_counts.tsv", 
                           delim = "\t", escape_double = FALSE, 
                           trim_ws = TRUE)

facet_counts <- facet_counts[facet_counts$facet != "phenotypic abnormality",]

# comparitive 
facet_counts_f <- data.frame(t(facet_counts[2:3]))
colnames(facet_counts_f) <- c(1,2,3)
colnames(facet_counts_f) <- unlist(facet_counts[,1])
facet_counts_f <- rbind(rep(0.15,20) , rep(0,20) , facet_counts_f)
colors_border=c(  rgb(160/255, 230/255, 230/255, maxColorValue = 1),rgb(0/255, 100/255, 100/255, maxColorValue = 1)
 )
colors_in=c( rgb(224/255, 255/255, 255/255, 0.5, maxColorValue = 1)
, rgb(0/255, 139/255, 139/255, 0.4, maxColorValue = 1)
)

png("pod.png", units="in", width=7, height=6, res=300)
par(mar = c(0,0,0,0))
radarchart(facet_counts_f,
            pfcol=colors_in , plwd=4 , plty=1,
           cglcol="grey", cglty=1, axislabcol="grey", axistype=1, 
           caxislabels = c('0%', '5%', '10%', '15%', '20%'),
           #caxislabels=seq(0,0.2,.05), 
           pcol=colors_border,
           cglwd=.8,
           vlcex=.9,
)
legend(x=.6, y=1.3, legend = c("Proportion of diseases", "Proportion of novel\n   phenotypes"), bty = "n", pch=20 , col=colors_in , text.col = "grey", cex=1, pt.cex=2)
dev.off()

library(readr)
library(heatmaply)
BiocManager::install("pheatmap")
BiocManager::install("xtable")

# Read the data
data <- read.csv('data/facet_by_disease/matrix.csv',  row.names = 1)

library(RColorBrewer)

library(htmlwidgets)
library(pheatmap)


mat <- as.matrix(data)
my_palette <- colorRampPalette(brewer.pal(9, "Blues"))(256)
mat <- log(mat+1)
mat_rev_row <- mat[nrow(mat):1, ]

# Reverse the order of columns
mat_rev_col <- mat[, ncol(mat):1]

# Reverse the order of both rows and columns
matrev <- mat[nrow(mat):1, 1:ncol(mat)]
pheatmap(matrev, 
              color = my_palette,
         
         border_color = NA,
         fontsize = 8,
         scale = "row",
              cluster_rows = FALSE,
              cluster_cols = FALSE)
dev.off()

s <- heatmaply(matrev, 
          colors = my_palette,
          labRow = rownames(matrev), 
          labCol = colnames(matrev), 
          xlab = "Phenotype Categories", 
          ylab = "Disease Categories", 
          Rowv = F,
          Colv = F,
          plot_method='ggplot',
          k_col=2,
          show_dendrogram = F)

ggsave(s)
saveWidget(hmplot, file = "heatmap.png", selfcontained = FALSE)

rownames(mat)
mat2 <- mat[1:14, 1:14]

diag_values <- diag(mat2)

# Extract the non-diagonal values
non_diag_values <- mat2[!row(mat2) == col(mat2)]

# Perform the t-test
t_test <- t.test(diag_values, non_diag_values)

# Print the results
print(t_test)

library(xtable)

flatten_mat <- as.vector(mat)

# Get the indices of the top 5 values
top_indices <- order(flatten_mat, decreasing = TRUE)[1:10]

# Get the top 5 values and their respective row and column names
top_values <- flatten_mat[top_indices]
top_row_names <- rownames(mat)[row(mat)[top_indices]]
top_col_names <- colnames(mat)[col(mat)[top_indices]]

# Create a data frame with the top values, row names, and column names
top_df <- data.frame(Value = top_values, Row = top_row_names, Column = top_col_names)

# Convert the data frame to a LaTeX table format
latex_table <- xtable(top_df, caption = "Top 5 Values", label = "tab:top5")

