library(readr)
responses <- read_delim("~/spvis/data/responses.tsv", 
                        "\t", escape_double = FALSE, trim_ws = TRUE)

library(ggplot2)
library(stringr)
library(gridExtra)

q1 = responses[responses$question == 'q1',]
q1$wrong =                   q1$response == 'Not established and definitely wrong'
  #q1$response == 'Not established and unlikely' |
            
q1$wrong[is.na(q1$wrong)] = TRUE
contingency_table <- table(q1$bldp, q1$wrong) 
                           
print(contingency_table)
result <- chisq.test(contingency_table)
print(result)

phi = cor(q1$smdp, q1$wrong, method = "pearson")
n <- length(q1$wrong)
t_statistic <- phi * sqrt((n - 2) / (1 - phi^2))

# Compute p-value (two-tailed)
p_value <- 2 * (1 - pt(abs(t_statistic), df = n - 2))
phi
p_value

precision = tp / tp + fp

nrow(q1[q1$wrong == F,]) / (nrow(q1[q1$wrong == F,]) + nrow(q1[q1$wrong == T,]))

q1bl = q1[q1$bldp == T,]
nrow(q1bl[q1bl$wrong == F,]) / (nrow(q1bl[q1bl$wrong == F,]) + nrow(q1bl[q1bl$wrong == T,]))

q1sm = q1[q1$smdp == T,]
nrow(q1sm[q1sm$wrong == F,]) / (nrow(q1sm[q1sm$wrong == F,]) + nrow(q1sm[q1sm$wrong == T,]))

extract_question <- function(q, smdp = F) {
  lvls <- unique(responses[responses$question == q,]$response)
  
  if(smdp) {
    df <- as.data.frame(table(responses[responses$question == q & 
                                      responses$smdp == T & responses$smdp_significant == T,]$response))
  } else {
    df <- as.data.frame(table(responses[responses$question == q & 
                                      responses$bldp == T,]$response))
  }
  
  names(df) <- c("Category", "Frequency")
  #df$Category <- factor(df, levels = lvls) This is actually only if we want to order it
  
  df$Proportion <- signif(df$Frequency / sum(df$Frequency), digits=3)
  
  return(df)
}
plot_vombo <- function(combo, title, subtitle) {
  return(ggplot(combo, aes(x = Category, y = Proportion, fill = factor(rep(c("SMDP", "BLDP"), each = length(combo$Category)/2)))) +
    geom_bar(stat = "identity", position = "dodge") +
    geom_text(aes(label = Proportion), position = position_dodge(width = 0.9), vjust = -0.5, size=2.5) +
    labs(title = title, subtitle = str_wrap(subtitle, width = 70),
         x = "Response", y = "Proportion", fill = "Source") +
    scale_fill_brewer(palette = "Set1") +  # Choose a color palette (e.g., Set1)
    theme_minimal() +  # Start with a minimal theme
    theme(
      text = element_text(family = "Arial", size = 12, color = "black"),
      axis.title.x = element_text(size = 14),
      axis.title.y = element_text(size = 14),
      plot.title = element_text(size = 16, face = "bold"),
      legend.title = element_text(size = 12),
      panel.background = element_rect(fill = "lightgray"),
      panel.grid.major = element_line(color = "white", linetype = "dotted"),
      panel.grid.minor = element_blank(),
      axis.line = element_line(color = "black", size = 0.5),
      axis.text = element_text(size = 10, angle = 45, hjust = 1),  # Rotate x-axis labels
      axis.ticks = element_line(color = "black", size = 0.5),
      legend.position = "right",
      legend.background = element_rect(fill = "white", color = "black"),
      legend.key = element_rect(fill = "white"),
      legend.text = element_text(size = 10),
      aspect.ratio = 1,  # Adjust the aspect ratio as needed
      plot.margin = margin(0, 0, 0, 00)  # Adjust the margins as needed
    ))
}
print_plot <- function(plot, name) {
  png(name, units="in", width=7, height=7, res=300)
  print(plot)
  dev.off()
}
g1 <- plot_vombo(rbind(extract_question('q1', T), extract_question('q1', F)), 
           "Proportion of Responses to Q1", 
           "\"Is this association established in literature, treatment guidelines, or policy discussing this disease?\"")
g2 <- plot_vombo(rbind(extract_question('q2', T), extract_question('q2', F)), 
           "Proportion of Responses to Q2", 
           "\"What kind of association is this?\"")
g3 <- plot_vombo(rbind(extract_question('q3', T), extract_question('q3', F)), 
           "Proportion of Responses to Q3", 
           "\"How often do you recognise this association in the course your clinical practice for this disease?\"")

print_plot(g1, 'Q1.png')
print_plot(g2, 'Q2.png')
print_plot(g3, 'Q3.png')

wilcox.test(extract_question('q3', T)$Proportion, 
            extract_question('q3', F)$Proportion, 
            paired= T)


sm_q3 <- extract_question('q3', T)
lvls <- unique(sm_q3$Category)
bl_q3 <- extract_question('q3', F)

dt_sm = rep(1:5, sm_q3$Frequency)
dt_bl = rep(1:5, bl_q3$Frequency)

wilcox.test(dt_sm, dt_bl, exact=FALSE, correct=FALSE, alternative="greater")



sm_q1 <- extract_question('q1', T)
lvls <- unique(sm_q1$Category)
lvls
bl_q1 <- extract_question('q1', F)

dt1_sm = rep(1:5, sm_q1$Frequency)
dt1_bl = rep(1:5, bl_q1$Frequency)

wilcox.test(dt1_sm, dt1_bl, exact=FALSE, correct=FALSE)


sm_q2 <- extract_question('q2', T)
lvls <- unique(sm_q2$Category)
bl_q2 <- extract_question('q2', F)
lvls
dt2_sm = rep(1:6, sm_q2$Frequency)
dt2_bl = rep(1:6, bl_q2$Frequency)

wilcox.test(dt2_sm, dt2_bl, exact=FALSE, correct=FALSE)