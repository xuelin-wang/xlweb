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

var FormsAppDispatcher = require('../dispatcher/FormsAppDispatcher');
var FormsConstants = require('../constants/FormsConstants');
var FormsWebAPIUtils = require('../utils/FormsWebAPIUtils');
var ChatMessageUtils = require('../utils/ChatMessageUtils');

var ActionTypes = FormsConstants.ActionTypes;

module.exports = {

  createMessage: function(text, currentThreadID) {
    FormsAppDispatcher.dispatch({
      type: ActionTypes.CREATE_MESSAGE,
      text: text,
      currentThreadID: currentThreadID
    });
    var message = ChatMessageUtils.getCreatedMessageData(text, currentThreadID);
    FormsWebAPIUtils.createMessage(message);
  }

};
