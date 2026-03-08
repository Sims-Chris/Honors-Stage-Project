# Honors-Stage-Project

## AI

1) Take the original test data and fill in missing data using MissForest py module
2) Validate the data using Isolation Forest
   "It builds random decision trees. Anomalies are easier to "isolate" (they require fewer splits in the tree), making them stand out from the normal clusters." - gemini
3) Build an AI that learns from this data and generates 10k new lines
4) Validate synthetic data
5) Combine 581 og data and 9500 synthetic data to get the final data set of **10,081**

## User inputs
We have these fields to compare with
bouldergrade_numeric,pullup reps_mean,pullup reps_median,pullup reps_std,pushup reps_mean,pushup reps_median,pushup reps_std,continuoushang_mean,continuoushang_median,continuoushang_std,maxhang_mean,maxhang_median,maxhang_std,weightedpull_mean,weightedpull_median,weightedpull_std,repeaters1_mean,repeaters1_median,repeaters1_std,bigmovecamp_mean,bigmovecamp_median,bigmovecamp_std,onerungcamp_mean,onerungcamp_median,onerungcamp_std,overheadprR_mean,overheadprR_median,overheadprR_std,overheadprL_mean,overheadprL_median,overheadprL_std,deadlift_mean,deadlift_median,deadlift_std,powl_mean,powl_median,powl_std,powr_mean,powr_median,powr_std,lhang/sit_mean,lhang/sit_median,lhang/sit_std,armjump_mean,armjump_median,armjump_std,hipjump_mean,hipjump_median,hipjump_std

So we have this data analysed:
Sport + boulder grade
Pull ups
Push ups
Continuous hang
Max hang
Weighted pull
Repeaters
Big move camp
One rung camp
Overhead Right
Overhead Left
Deadlift
Power left
Power Right
Lhang / sit
Arm jump
Hip jump

We are going to use this data, for our user inputs:
Grade
Pull ups
Push ups
Continuous hang (20mm)
Max hang, (10 seconds, 20mm, pounds)
1 Rep max weighted pull up (lb)
Repeaters
Explosive campus (Inches) (Power left, Power Right)

Then we also have:
Flexibility (scale of how much they want 0-10)
Length of time
Sessions per week
