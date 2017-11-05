# quick-itinerary-cost-calc
Quick cost guesstimate calculator for out-of-state hackathons

# Functionality
Uses Google's QPX Express API to calculate flight cost of a round trip with an individual person
Uses Google's Geocoder API to figure out the latitude/longitude of an airport code
Uses Lyft's API to find distance from departing address to departing airport and destination airport to destination address

![Alt text](https://user-images.githubusercontent.com/5902976/32418562-b5a0a20a-c229-11e7-8a4b-c4f83317316b.png "Screenshot of app")

# Things to improve
The networking code sucks. I know. I didn't understand how to use AsyncTask when I wrote this code and it looks like a mess. It would be best to rewrite it with multiple AsyncTasks that do any UI operations onPostExecute().
The UI could use a huge lift.
I didn't create a git repository until after I finished the initial code but I should started the project with a git repository and committed everything along the way.
