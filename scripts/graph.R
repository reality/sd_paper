
if (!require("RgraphViz", quietly=T))
  BiocManager::install("Rgraphviz")

if (!require("ontologyPlot", quietly=T))
  BiocManager::install("ontologyPlot")

install.packages('ontologyPlot')
library(ontologyIndex)
library(ontologyPlot)
data(hpo)

onto_plot(hpo, terms=get_descendants(hpo, roots=c("HP:0012531")))

library(igraph)
library(dplyr)
library(readr)
vertices <- read_delim("data/create_graph_dataframe/vertices.tsv", 
                       "\t", escape_double = FALSE, col_names = FALSE, 
                       trim_ws = TRUE)
graph_df <- read_delim("data/create_graph_dataframe/graph_df.tsv", 
                       "\t", escape_double = FALSE, trim_ws = TRUE)

g <- graph.data.frame(graph_df, directed=TRUE)

ordered_values <- c(vertices$X2[match(V(g)$name, vertices$X1)], 
                    vertices$X3[match(V(g)$name, vertices$X1)])
#ordered_values[1] = 0

max_log_value <- log(max(ordered_values))
min_log_value <- log(min(ordered_values[ordered_values > 0])) 

pal <- colorRampPalette(c("white", "red"))(length(unique(ordered_values))) # Here, 100 can be any number based on the granularity of colors you want

# Function to map log-transformed values to color palette
map_value_to_color <- function(value, palette, min_val, max_val) {
  index <- ifelse(value == -Inf,
                  1,
                  round((length(palette) - 1) * (value - min_val) / (max_val - min_val) + 1))
  print(index)
  return(palette[index])
}

# Map the log-transformed values to colors
color_vector <- sapply(log(ordered_values), map_value_to_color, 
                       palette = pal, min_val = min_log_value, max_val = max_log_value)
color_vector
# Map the log-transformed values to colors

#ordered_values[ordered_values == 1] <- 0.25
#vertex_colours <- pal[ordered_values]
vertex_colours_bldp <- color_vector[1:(length(color_vector)/2)]
vertex_colours_smdp <- color_vector[((length(color_vector)/2)+1):length(color_vector)]
#vertex_colours[1] = "#0000F0"
length(vertex_colours_bldp)
length(vertex_colours_smdp)


set.seed(204205520)
#png("Pain-Network-BLDP.png", units="in", width=7, height=6, res=300)
par(mar = c(0, 0, 0, 0),bg = "white")
plot(g, layout=layout_with_lgl(g), vertex.size=4, vertex.size2=5,
       edge.size=5, edge.arrow.size = 0.5, vertex.shape='circle', asp=1, vertex.label.cex=0.5, 
     vertex.label.dist=c(0.75), vertex.label.degree=c(-pi/2,pi/2), vertex.color=vertex_colours_bldp,
     rescale=T)
#dev.off()

set.seed(204205520)
#png("Pain-Network-SMDP.png", units="in", width=7, height=6, res=300)
par(mar = c(0, 0, 0, 0),bg = "white")
plot(g, layout=layout_with_lgl(g), vertex.size=4, vertex.size2=5,
     edge.size=5, edge.arrow.size = 0.5, vertex.shape='circle', asp=1, vertex.label.cex=0.5, 
     vertex.label.dist=c(0.75), vertex.label.degree=c(-pi/2,pi/2), 
     vertex.color=vertex_colours_smdp,
     rescale=T)
#dev.off()
