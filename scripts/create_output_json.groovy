import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import groovy.json.*

def labels = [:]
new File('../synonym_expansion_validation/do/disease_unexpanded.txt').splitEachLine('\t') {
  it[1] = it[1].tokenize('/').last().replace('_',':')
  if(!labels.containsKey(it[1])) { labels[it[1]] = it[0] }
}
new File('../synonym_expansion_validation/hpo/unexpanded_all.txt').splitEachLine('\t') {
  it[1] = it[1].tokenize('/').last().replace('_',':')
  if(!labels.containsKey(it[1])) { labels[it[1]] = it[0] }
}

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
    bldp: [:], 
    smdp: [:] 
  ]
  novel[it[0]].each {
    output[it[0]].smdp[it[2]] = [
      label: labels[it[2]],
      laconic: it[4].tokenize(':')[1] == 'true',
      novel: it[5].tokenize(':')[1] == 'true',
      npmi: Float.parseFloat(it[6].tokenize(':')[1]),
      significant: it[7].tokenize(':')[1] == 'TRUE',
    ]
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

new File('data/create_output_json/data.json').text = new JsonBuilder(fullOut).toPrettyString()
