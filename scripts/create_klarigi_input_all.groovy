import groovy.json.*

def allScores = new JsonSlurper().parseText(new File('data/create_output_json/data.json').text).profiles
def out = []
def c = 0
allScores.each { doid, assoc ->
  assoc.smdp.findAll { k, v -> v.novel && v.significant  }.each { k, v ->
    out << "${c++}\t$k\tall"
  }
}
new File('data/klarigi_input/input.tsv').text = out.join('\n')
