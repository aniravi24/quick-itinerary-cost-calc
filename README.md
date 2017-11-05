# quick-itinerary-cost-calc
Quick cost guesstimate calculator for out-of-state hackathons

# Functionality
1. Uses Google's QPX Express API to calculate flight cost of a round trip with an individual person
2. Uses Google's Geocoder API to figure out the latitude/longitude of an airport code
3. Uses Lyft's API to find distance from departing address to departing airport and destination airport to destination address

# How to use this app
1. Import this project into Android Studio
2. Open your gradle.properties file and set the following properties <br>
LYFT_CLIENT_ID <br>
LYFT_CLIENT_TOKEN <br>
GOOGLE_GEOCODER_APIKEY <br>
GOOGLE_PLACES_APIKEY <br>
GOOGLE_QPXEXPRESS_APIKEY <br>
Example: <br>
LYFT_CLIENT_ID = "insert your API key here" <br>

![Alt text](https://user-images.githubusercontent.com/5902976/32418562-b5a0a20a-c229-11e7-8a4b-c4f83317316b.png "Screenshot of app")

# Things to improve
1. The networking code sucks. I know. I didn't understand how to use AsyncTask when I wrote this code and it looks like a mess. It would be best to rewrite it with multiple AsyncTasks that do any UI operations onPostExecute().
2. The UI could use a huge lift.
3. I didn't create a git repository until after I finished the initial code but I should started the project with a git repository and committed everything along the way.
4. Move off Google's QPX Express API since it is set to shut down in April 2018.
