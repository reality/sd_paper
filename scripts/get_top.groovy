import groovy.json.*

def allScores = new JsonSlurper().parseText(new File('data/create_output_json/data.json').text).profiles
def counts = [:]
def oCount = 0
allScores.each { doid, assoc ->
  assoc.smdp.findAll { k, v -> v.novel && v.significant }.each { k, v ->
    k = k+':'+v.label
    if(!counts.containsKey(k)) { counts[k] = 0 } 
    counts[k]++
    oCount++
  }
}

println counts.sort { -it.value }.take(15)

println oCount
