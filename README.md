# Order of execution

## groovy scripts/format.groovy > data/raw_transactions.tsv

This interpolates the raw CSV files provided by WS into a single file.

We will also edit it to remove the DOIDs that don't exist from the file.

Result: data/raw_transactions.tsv

## groovy scripts/process_transactions.groovy

## groovy scripts/run_permutations.groovy

## groovy scripts/process_associations.groovy

## groovy scripts/format_class_for_ic.groovy

## sh ./scripts/get_ic.sh

## QVals in R

Run the R script!

## groovy find_explicit_nonmatch.groovy

## groovy scripts/create_output_json.groovy
