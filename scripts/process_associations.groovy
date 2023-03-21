def ic = [:]
new File('data/get_ic/phens_ic.tsv').splitEachLine('\t') {
  ic[it[0].tokenize('/').last().replace('_',':')] = Float.parseFloat(it[1])
}
def IC_CUTOFF = 0

println "Loading associations..."

def totalTests = 0
def associations = [:]
def totalAssociations = 0
new File('data/process_transactions/associations.tsv').splitEachLine('\t') {
  if(!associations.containsKey(it[0])) { associations[it[0]] = [:] }
  if(it[3] == 'NaN') { return; }
  def val = Double.parseDouble(it[3])
  if(val > 0) { 
    associations[it[0]][it[1]] = [ value: val, perms: [ val ] ]; 
    totalAssociations++;
  }
}

println "Total positive associations: ${totalAssociations} for ${associations.size()} diseases"
println "Loading counts..."

def counts = [:]
new File('data/process_transactions/counts.tsv').splitEachLine('\t') {
  def val = Double.parseDouble(it[1])
  counts[it[0]] = [ value: val, perms: [ val ] ]
}

def oTotal = counts.collect { it.getValue().value }.sum()
counts.each {
  def propT = it.getValue().value / oTotal
  it.getValue().value = propT
  it.getValue().perms[0] = propT
}

println "Loaded ${counts.size()} classes with $oTotal transacs."

println 'Loading permutations'
def assocFiles = []
def countFiles = []
new File('data/run_permutations/').eachFile { f ->
  if(f.getName() =~ /associations/) { assocFiles << f }
  if(f.getName() =~ /counts/) { countFiles << f  }
}

def fileCounts = [:]
countFiles.eachWithIndex { f, o ->
  if((o % 10) == 0) { println "$o/${countFiles.size()}" } 
  fileCounts[f.getName()] = []

  f.splitEachLine('\t') {
    it[1].tokenize(',').each { fileCounts[f.getName()] << [:] }
    it[1].tokenize(',').eachWithIndex { pv, i ->
      def procVal = 0
      if(pv != 'NaN') { procVal = Double.parseDouble(pv) }
      fileCounts[f.getName()][i][it[0]] = procVal
    }
  }
}

fileCounts.each { fn, ps ->
  ps.each { cs ->
    def total = cs.collect { it.getValue() }.sum()
    cs.each { k, v ->
      counts[k].perms << v/total
    }
  }
}

println "Finished loading count permutations..."
def zout = new PrintWriter(new BufferedWriter(new FileWriter("data/process_associations/counts.tsv")))
def COUNT_P_CUTOFF = 0.001
def icR = 0
cutoff_counts = counts.each { k, v ->
  //if(ic.containsKey(k) && ic[k] <= IC_CUTOFF) { icR++ ; return false; } 

  def pVal = new Double(v.perms.findAll { it >= v.value }.size() / v.perms.size()).round(4)

  zout.println("$k\t$pVal")

  totalTests++
/*
  if(k =~ /HP/) { return true;}
  if(!(k =~ /DOID/)) { return false; }

  totalTests++
  //pVal <= COUNT_P_CUTOFF ? true : false
}*/
}
zout.flush() ; zout.close()

println "classes with significant counts: " + cutoff_counts.size() + " and $totalTests test so far. also removed $icR phens with low ic"

// loading the other things
assocFiles.eachWithIndex { f, i ->
  if((i % 10) == 0) { println "$i/${assocFiles.size()}" }
  f.splitEachLine('\t') {
    if(!cutoff_counts.containsKey(it[0])) { return; }
    if(!cutoff_counts.containsKey(it[1])) { return; }
    it[2].tokenize(',').each { pv ->
      def procVal = 0
      if(pv != 'NaN') { procVal = Float.parseFloat(pv) }
      associations[it[0]][it[1]].perms << procVal
    }
  }
}

println "Finished loading association permutations..."

println "Loaded permutations"

println "Attaching p values to associations and writing..."
def aout = new PrintWriter(new BufferedWriter(new FileWriter("data/process_associations/associations.tsv")))
def aCount = 0
def sigCount = 0
associations.each { d, ps ->
  if(!cutoff_counts.containsKey(d)) { return; }
  ps.each { p, v ->
    if(!cutoff_counts.containsKey(p)) { return; }
    aCount++
    totalTests++
    def pVal = new Double(v.perms.findAll { it >= v.value }.size() / v.perms.size()).round(4)
    if(pVal <= COUNT_P_CUTOFF) { sigCount++ }
    aout.println("$d\t$p\t${v.value}\t$pVal") 
  }
}

aout.flush() ; aout.close()

println "done. assoc count: $aCount. sig count: $sigCount. test: $totalTests"
