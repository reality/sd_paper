sim_class <- read.delim("data/create_ws_dis-dis_network/ws_dis_sim.tsv", header = F)

sim_class$V4 <- as.factor(sim_class$V4)

print("enrichment")
AUC::roc(sim_class$V4, sim_class$V3)


sim_class <- NULL
a <- hist(sim_class$V3, freq=F)

# take a look at the matching subset
ms <- sim_class[sim_class$V4 == "true",]

a <- hist(ms$V3)
plot(a)

ms2 <- ms[ms$V3 <= 1,]
library(pROC)

auc(sim_class$V4, sim_class$V3)
ci(roc(sim_class$V4, sim_class$V3))
