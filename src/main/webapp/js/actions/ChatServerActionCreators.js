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

var AppDispatcher = require('../dispatcher/AppDispatcher');
var XlwebConstants = require('../constants/XlwebConstants');

var ActionTypes = XlwebConstants.ActionTypes;

module.exports = {

  receiveAll: function(rawMessages) {
    AppDispatcher.dispatch({
      type: ActionTypes.RECEIVE_RAW_MESSAGES,
      rawMessages: rawMessages
    });
  },

  receiveCreatedMessage: function(createdMessage) {
    AppDispatcher.dispatch({
      type: ActionTypes.RECEIVE_RAW_CREATED_MESSAGE,
      rawMessage: createdMessage
    });
  }

};
