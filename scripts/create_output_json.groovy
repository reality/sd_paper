import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import groovy.json.*

def toRemove = new File('data/review/correctness.tsv').text.split('\n').findAll { it.split('\t')[1] == 'Y' }.collect { it.split('\t')[0] }

def omims = new File('data/extract_crossrefs_and_labels/doid.tsv').text.split('\n').collect { it.tokenize('\t') }.findAll { it[1] =~ /OMIM/ }.collect { it[0] = it[0].split('/').last().replace('_', ':') ; it }
new File('data/omim_mappings.tsv').text = omims.collect { "${it[0]}\t${it[1]}" }.join('\n')

def hpoa = [:]
new File('data/phenotype.hpoa').splitEachLine('\t') {
  if(!hpoa.containsKey(it[0])) { hpoa[it[0]] = [] }
  hpoa[it[0]] << it[3]
}

def maps = [:]
new File('data/match_litphens/doid_mappings.tsv').splitEachLine('\t') {
  maps[it[0]] = it[1] ? it[1].tokenize(';') : []
}

def labels = [:]
new File('../synonym_expansion_validation/do/disease_unexpanded.txt').splitEachLine('\t') {
  it[1] = it[1].tokenize('/').last().replace('_',':')
  if(!labels.containsKey(it[1])) { labels[it[1]] = it[0] }
}
new File('../synonym_expansion_validation/hpo/unexpanded_all.txt').splitEachLine('\t') {
  it[1] = it[1].tokenize('/').last().replace('_',':')
  if(!labels.containsKey(it[1])) { labels[it[1]] = it[0] }
}

def ukbb = [:]
new File('..//data_portal/data/qval/associations.tsv').splitEachLine('\t') {
  if(it[0] == 'X1') { return; }
  def doid = it[0].replace('_', ':')
  def hpo = it[1].replace('_', ':')

  if(!ukbb.containsKey(doid)) { ukbb[doid] = [:] }
  ukbb[doid][hpo] = [
    id: hpo,
    label: labels[hpo],
    significant: it[5] == true,
    npmi: it[2]
  ]
}

// it's not really 'novel' this is a list of all of them
def novel = [:]
new File('./data/find_explicit_nonmatch/explicit_smonly.tsv').splitEachLine('\t') {
  if(!novel.containsKey(it[0])) { novel[it[0]] = []}
  novel[it[0]] << it 
}

def output = [:]
new File('./data/find_explicit_nonmatch/matched_profiles.tsv').splitEachLine('\t') {
  output[it[0]] = [ 
    id: it[0],
    label: labels[it[0]],
    mappings: maps[it[0]],
    bldp: [:], 
    smdp: [:],
    ukbb: [:],
    hpoa: []
  ]

  def omimMatch = omims.find { o -> it[0] == o[0] }
  if(omimMatch) {
    output[it[0]].hpoa = hpoa[omimMatch[1]]
  }

  novel[it[0]].each {
    if(toRemove.contains(it[2])) { return; }
    output[it[0]].smdp[it[2]] = [
      label: labels[it[2]],
      laconic: it[4].tokenize(':')[1] == 'true',
      novel: it[5].tokenize(':')[1] == 'true',
      npmi: Float.parseFloat(it[6].tokenize(':')[1]),
      significant: it[7].tokenize(':')[1] == 'TRUE',
    ]
  }
  if(ukbb.containsKey(it[0])) {
    output[it[0]].ukbb = ukbb[it[0]]
  }
  if(it[2] != 'false') {
    it[2].tokenize(';').each { p -> output[it[0]].bldp[p] = [ label: labels[p] ] }
  }
}

LocalDateTime now = LocalDateTime.now()
DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd/HH:mm:ss")
String formattedDateTime = now.format(formatter)

def fullOut = [
  versionString: formattedDateTime, 
  author: 'https://orcid.org/0000-0001-9227-0670',
  profiles: output
]

println 'huh?'
new File('data/create_output_json/data.json').text = new JsonBuilder(fullOut).toPrettyString()
