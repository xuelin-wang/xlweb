/**
 * This file is provided by Facebook for testing and evaluation purposes
 * only. Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

// This file bootstraps the entire application.

var XlwebApp = require('./components/XlwebApp.react');
var ChatExampleData = require('./ChatExampleData');
var FormsWebAPIUtils = require('./utils/FormsWebAPIUtils');
var React = require('react');
window.React = React; // export for http://fb.me/react-devtools

ChatExampleData.init(); // load example data into localstorage

FormsWebAPIUtils.getAllMessages();

React.render(
    <XlwebApp />,
    document.getElementById('forms')
);
