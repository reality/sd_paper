# Order of execution

## Processing literature phenotypes

You can skip this phase, but if you want to regenerate the literature phenotypes and their mappings to DOID, you can run the following:

### groovy scripts/process_litphens.groovy

## groovy scripts/format.groovy > data/raw_transactions.tsv

This interpolates the raw CSV files provided by WS into a single file.

We will also edit it to remove the DOIDs that don't exist from the file.

Result: data/raw_transactions.tsv

## groovy scripts/process_transactions.groovy

This takes the data/raw_transactions.tsv, propagates phenotypes, calculates NPMI, creates transaction profile for permutations.

## groovy scripts/run_permutations.groovy

## groovy scripts/process_associations.groovy

## groovy scripts/format_class_for_ic.groovy

## sh ./scripts/get_ic.sh

## QVals in R

Run the R script!

## groovy scripts/find_explicit_nonmatch.groovy

## groovy scripts/create_output_json.groovy

# Subsequent analysis

## groovy scripts/create_facet_counts.groovy
## groovy scripts/create_disease_facet_counts.groovy

## analysis.R 

This contains the code to look at perplexity, facets, radarcharts etc.

## groovy scripts/create_ws_dis-dis_network.groovy

This will create the similarity matrix between diseases in the BL-DP and the SM-DP.

## groovy scripts/get_group_ic.groovy

calculate average IC for constitutional sympyoms

## groovy scripts/create_klarigi_input_all.groovy

# Clinical Review

We don't include the original JSON files with the responses, but the data sheet that it produces is stored in *data/review/responses.tsv*

# Table 2

This is produced using Klarigi 

klarigi --debug --data data/create_facet_counts/smdp_constitutional.tsv -o data/hp.owl --verbose --output-type=latex --output-scores --scores-only --egl --min-exclusion=0 --max-inclusion=0.9 --min-ic=0.4 --min-inclusion=0
