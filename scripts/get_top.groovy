import groovy.json.*

def allScores = new JsonSlurper().parseText(new File('data/create_output_json/data.json').text).profiles
def counts = [:]
allScores.each { doid, assoc ->
  assoc.smdp.findAll { k, v -> v.novel }.each { k, v ->
    k = k+':'+v.label
    if(!counts.containsKey(k)) { counts[k] = 0 } 
    counts[k]++
  }
}

println counts.sort { -it.value }.take(15)

println counts['HP:0012531:pain']
