require('dotenv').config()
process.env.DEBUG='provider:*'
const fs = require('fs');

let platforms = fs.readFileSync('lms_platforms.json');
platforms = JSON.parse(platforms);

let lti = require('ltijs').Provider
lti = lti.setup(process.env.LTI_KEY,
  {
    url: 'mongodb://' + process.env.MONGO_HOST + '/' + process.env.MONGO_INITDB_DATABASE + '?authSource=admin',
    connection: { user: process.env.MONGO_INITDB_ROOT_USERNAME, pass: process.env.MONGO_INITDB_ROOT_PASSWORD}
  }, {
    appRoute: '/bazaar', loginRoute: '/bazaar/ltilogin', keysetRoute: '/bazaar/keys',
    sessionTimeoutRoute: '/bazaar/sessiontimeout', invalidTokenRoute: '/bazaar/invalidtoken',
    cookies: {
      secure: false, // Set secure to true if the testing platform is in a different domain and https is being used
      sameSite: '' // Set sameSite to 'None' if the testing platform is in a different domain and https is being used
    },
    devMode: false // Set DevMode to true if the testing platform is in a different domain and https is not being used
  });

const delay = async (ms) => {
  return new Promise(resolve => setTimeout(resolve, ms));
}

const setup = async () => {
  try {
    await lti.Database.setup();

    /**
     * Register platform
     */
    const register = async () => {
      return Promise.all(platforms.map(item => lti.registerPlatform(item)))
    }
    register().then((plat)=>{
      console.log(JSON.stringify(plat));
    }).catch((err)=>{
      console.log("register errors ", err);
    })
    await delay(3000);
    await lti.Database.Close();
    process.exit()

  } catch (err) {
    console.log('Error during deployment: ', err)
    await lti.Database.Close()
    process.exit()
  }
}
setup();