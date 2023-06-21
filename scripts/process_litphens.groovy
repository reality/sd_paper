def mappings = [
  'DOID:3451': ['ICD10:C44.9'],
  'ICD10:C44.9': ['DOID:3451'],

  'DOID:4451': ['ICD10:C64.9'],
  'ICD10:C64.9': ['DOID:4451'],

  'DOID:0080784': ['ICD10:N39.0'],
  'ICD10:N39.0': ['DOID:0080784'],

  'DOID:0070004': ['ICD10:C92'],
  'ICD10:C92': ['DOID:0070004'],

  'DOID:4001': ['ICD10:C56'],
  'ICD10:C56': ['DOID:4001'],

  'DOID:0060060': ['ICD10:C85.9'],
  'ICD10:C85.9': ['DOID:0060060'],

  'DOID:0050861': [ 'ICD10:C18.9' ],
  'ICD10:C18.9': ['DOID:0050861'],

  'DOID:1542': ['ICD10:C76.0'],
  'ICD10:C76.0': ['DOID:1542'],

  'DOID:4667': ['ICD10:M40.209'],
  'ICD10:M40.209': ['DOID:4667'],

'DOID:10247': ['ICD10:R09.1'],
'ICD10:R09.1': ['DOID:10247'],

'DOID:9909': ['ICD10:H00.019'],
'ICD10:H00.019': ['DOID:9909'],
]

def processMappingInput = { it ->
  it = it.split('\t')
  //if(it[1] =~ /\.9$/) { it[1] = it[1].replaceAll(/\.9/, "") }
  if(it[1] =~ /_/) { it[1] = it[1].tokenize('/').last().replace('_',':') }
  if(it[0] =~ /_/) { it[0] = it[0].tokenize('/').last().replace('_',':') }

  // Add mappings in both directions
  if(!mappings.containsKey(it[0])) { mappings[it[0]] = [] }
  mappings[it[0]] << it[1]
  if(!mappings.containsKey(it[1])) { mappings[it[1]] = [] }
  mappings[it[1]] << it[0]

}
new File('./data/extract_crossrefs_and_labels/mondo.tsv').text.split('\n').each { processMappingInput(it) }
new File('./data/extract_crossrefs_and_labels/doid.tsv').text.split('\n').each { processMappingInput(it) }

def rawLitDiseases = [:]
// Load literature associations
new File('data/bldp_raw/TM.umls_icd-pheno.txt').splitEachLine('\t') { f ->
  // The UMLS or the label for this mapping 
  def umls = f[0].toUpperCase()
  
  if(!rawLitDiseases.containsKey(umls)) {
    def labels = f[1].replaceAll('\\[','').replaceAll('\\]','').tokenize(',').collect { "label:${it.trim().toLowerCase()}" }
    def icds = f.last().tokenize('#').collect { icd ->
      /*if(icd =~ /\.9$/) {
        icd = icd.replaceAll(/\.9/, "")
      }*/
      icd 
    }

    rawLitDiseases[umls] = [ keys: [ 
      umls: umls,
      labels: labels,
      icd: icds
    ], phens: [:] ]
  }

  def hp = f[2]
  if(hp =~ /HP/) {
    rawLitDiseases[umls].phens[hp] = true
  }
}

addedMappings = []
new File('data/bldp_raw/semiautomatic_ICD-pheno.txt').splitEachLine('\t') { f ->
  if(f[0] == 'ICD10 Code') { return; }
  def icd = 'ICD10:' + f[0]
  /*if(icd =~ /\.9$/) {
    icd = icd.replaceAll(/\.9/, "")
  }*/

  if(!rawLitDiseases.containsKey(icd)) {
    def labels = f[1].tokenize(';').collect { 
      it.trim().tokenize(':')
    }.findAll {
      it[0] == f[0] && it.size() > 1 
    }.collect { 
      "label:"+it[1].trim().toLowerCase() 
    }
 
    rawLitDiseases[icd] = [ keys: [ 
      labels: labels
    ], phens: [:] ]   
  }

  def hp = f[2]
  if(hp =~ /HP/) {
    rawLitDiseases[icd].phens[hp] = true
  }
}
new File('data/bldp_raw/PheneBank_Associations.tsv').splitEachLine('\t') { f ->
  def mondo = f[1]
  def hp = f[3]
  def label = 'label:' + f[0] 

  if(!rawLitDiseases.containsKey(mondo)) { 
    rawLitDiseases[mondo] = [ keys: [ 
      labels: [label]
    ], phens: [:] ]
  }

  rawLitDiseases[mondo].phens[hp] = true
}

def litAssoc = [:]
def i = 0
def unMapped = []
def mappingOut = [:]
// so here we have a mix of MONDOs and ICDs in MONDOs and ICDs
rawLitDiseases.each { key, d ->
  println "investigating $key (${++i}/${rawLitDiseases.size()})"
  //if(!d) { println 'nulliam' ; return }

  def doids = []
  
  if(mappings.containsKey(key)) { // direct mapping
    doids += mappings[key].findAll { it =~ /DOID/ }
  } else {
    d.keys.each { k, vv ->
      vv.each { v -> 
        if(mappings.containsKey(v)) {
          doids += mappings[v].findAll { it =~ /DOID/ } 
        }
      }
    } 
  }

  doids.unique(true)
  doids.each {
    if(!mappingOut.containsKey(it)) { mappingOut[it] = [] }
    mappingOut[it] << key+':'+d.phens.size()
  }

  println doids
  println "total unique mappings ${doids.size()}"
  println ""
  doids.each { doid ->
    doid = doid.tokenize('/').last().replace('_',':')
    if(!litAssoc.containsKey(doid)) { litAssoc[doid] = [:] }
    d.phens.each { k, v ->
      litAssoc[doid][k] = true
    }
  }

  if(doids.size() == 0) {
    unMapped << key 
  }
}

println 'done with the mapching'

new File('./data/match_litphens/unmapped.txt').text = unMapped.join('\n')
new File('./data/match_litphens/doid_mappings.tsv').text = mappingOut.collect { k, v -> "$k\t${v.join(';')}" }.join('\n')

def mout = new PrintWriter(new BufferedWriter(new FileWriter("./data/match_litphens/final_mappings.tsv")))
litAssoc.each { doid, hpMap ->
  hpMap.each { k, v ->
      mout.println("$doid\t$k")
  }
}
mout.flush() ; mout.close()
