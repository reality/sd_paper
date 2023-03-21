def diseases = []
def phenotypes = []
new File('data/doid_lit_withphenebank.tsv').splitEachLine('\t') {
  diseases << it[0]  
  phenotypes << it[1]
}
new File('data/process_associations/counts.tsv').splitEachLine('\t') {
 if(it[0] =~ /DOID/) { diseases << it[0] }
 if(it[0] =~ /HP/) { phenotypes << it[0] } 
}

new File('data/format_class_for_ic/phens.tsv').text = "0\t${phenotypes.join(';')}\ttrue"
new File('data/format_class_for_ic/dises.tsv').text = "0\t${diseases.join(';')}\ttrue"
