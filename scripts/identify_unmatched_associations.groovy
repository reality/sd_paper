def smAssoc = [:]
new File('./data/qval/associations.tsv').splitEachLine('\t') {
  //if(PARENTFIX && ic[it[1]] < IC_CUTOFF) { return; }
  if(it[0] == 'X1') { return; }
  //if(litAssoc.containsKey(it[0])) { matched = true }
  def pmi = Double.parseDouble(it[2])
  if(pmi > 0) { 
    if(!smAssoc.containsKey(it[0])) { smAssoc[it[0]] = [] }
    smAssoc[it[0]] << [ it[1], pmi, it[5] ]
  } 
}

def litAssoc = [:]
new File('./data/match_litphens/final_mappings.tsv').splitEachLine('\t') {
  if(!litAssoc.containsKey(it[0])) {
    litAssoc[it[0]] = []
  }
  litAssoc[it[0]] << it[1]
}
litAssoc.collectEntries { k, v -> [(k): v.unique(false)] }

smAssoc.each { k, v ->
  if(!litAssoc.containsKey(k)) {
    println 'unmapped: ' + k
  }
}
