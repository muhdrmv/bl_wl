/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

(function initExamplePlayer() {

    /**
     * The URL of the Guacamole session recording which should be played back.
     *
     * @constant
     * @type String
     */

    // @rjp-004
    function getParameterByName(name, url) {
        if (!url) url = window.location.href;
        name = name.replace(/[\[\]]/g, '\\$&');
        var regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)'),
            results = regex.exec(url);
        if (!results) return null;
        if (!results[2]) return '';
        return decodeURIComponent(results[2].replace(/\+/g, ' '));
    }

    // @rjp-007
    var getParamS = getParameterByName("s");
    var getParamF = getParameterByName("f");

    function handleSftpView(getParamF) {

        // ajax get log file
        // Set up our HTTP request
        var xhr = new XMLHttpRequest();

        // Setup our listener to process completed requests
        xhr.onload = function () {

            // Process our return data
            if (xhr.status >= 200 && xhr.status < 300) {
                // What do when the request is successful
                console.log('success!', xhr);
                renderSftpView(xhr.responseText);
            } else {
                console.log('The request failed!');
            }
        };

        xhr.open('GET', "file-loader?f=" + getParamF);
        xhr.send();
    }

    function renderSftpView(sftpRawLog) {
        // parse log
        var sftpRecords = [];
        var sftpRows = sftpRawLog.split("\n");
        for (i = 0; i < sftpRows.length; ++i) {
            var row = sftpRows[i];
            if (row === "") continue;
            var sftpRecordFields = row.split("|");
            sftpRecords.push({"time": sftpRecordFields[0], "verb": sftpRecordFields[1], "dir": sftpRecordFields[2]});
        }
        console.log(sftpRecords);
        // render to table
        var sftpplayercont = document.getElementById("sftp-player");
        var trHtml = "<table><thead><th>Time</th><th>Action</th><th>Directory</th></thead>";
        for (i = 0; i < sftpRecords.length; ++i) {
            var record = sftpRecords[i];
            record.stime = new Date(record.time * 1)
            trHtml += "<tr>";
            trHtml += "<td>" + record.stime.toLocaleString('en-GB', { hour12:false } ) + "</td>";
            trHtml += "<td><span class=" + record.verb + ">" + record.verb + "</span></td>";
            trHtml += "<td>" + record.dir + "</td>";
            trHtml += "</tr>";
        }
        trHtml += "</table>";
        sftpplayercont.innerHTML = trHtml;

    }

    if (getParamS == null) {
        var playerEl = document.getElementById("player");
        playerEl.style.display = "none";
        if (getParamF !== null) {
            handleSftpView(getParamF);
        }
        return;
    }

    // @rjp-101
    var fileUrl = atob(getParamS);
    var RECORDING_URL = fileUrl;

    // var RECORDING_URL = "file-loader?s=" + getParamS;
    // var RECORDING_URL = 'recording.guac';

    /**
     * The element representing the session recording player.
     *
     * @type Element
     */
    var player = document.getElementById('player');

    /**
     * The element which will contain the recording display.
     *
     * @type Element
     */
    var display = document.getElementById('display');

    /**
     * Play/pause toggle button.
     *
     * @type Element
     */
    var playPause = document.getElementById('play-pause');

    /**
     * Button for cancelling in-progress seek operations.
     *
     * @type Element
     */
    var cancelSeek = document.getElementById('cancel-seek');

    /**
     * Text status display indicating the current playback position within the
     * recording.
     *
     * @type Element
     */
    var position = document.getElementById('position');

    /**
     * Slider indicating the current playback position within the recording,
     * and allowing the user to change the playback position.
     *
     * @type Element
     */
    var positionSlider = document.getElementById('position-slider');

    /**
     * Text status display indicating the current length of the recording.
     *
     * @type Element
     */
    var duration = document.getElementById('duration');

    /**
     * The tunnel which should be used to download the Guacamole session
     * recording.
     *
     * @type Guacamole.Tunnel
     */
    var tunnel = new Guacamole.StaticHTTPTunnel(RECORDING_URL);

    /**
     * Guacamole.SessionRecording instance to be used to playback the session
     * recording.
     *
     * @type Guacamole.SessionRecording
     */
    var recording = new Guacamole.SessionRecording(tunnel);

    /**
     * The Guacamole.Display which displays the recording during playback.
     *
     * @type Guacamole.Display
     */
    var recordingDisplay = recording.getDisplay();

    /**
     * Converts the given number to a string, adding leading zeroes as necessary
     * to reach a specific minimum length.
     *
     * @param {Numer} num
     *     The number to convert to a string.
     *
     * @param {Number} minLength
     *     The minimum length of the resulting string, in characters.
     *
     * @returns {String}
     *     A string representation of the given number, with leading zeroes
     *     added as necessary to reach the specified minimum length.
     */
    var zeroPad = function zeroPad(num, minLength) {

        // Convert provided number to string
        var str = num.toString();

        // Add leading zeroes until string is long enough
        while (str.length < minLength)
            str = '0' + str;

        return str;

    };

    /**
     * Converts the given millisecond timestamp into a human-readable string in
     * MM:SS format.
     *
     * @param {Number} millis
     *     An arbitrary timestamp, in milliseconds.
     *
     * @returns {String}
     *     A human-readable string representation of the given timestamp, in
     *     MM:SS format.
     */
    var formatTime = function formatTime(millis) {

        // Calculate total number of whole seconds
        var totalSeconds = Math.floor(millis / 1000);

        // Split into seconds and minutes
        var seconds = totalSeconds % 60;
        var minutes = Math.floor(totalSeconds / 60);

        // Format seconds and minutes as MM:SS
        return zeroPad(minutes, 2) + ':' + zeroPad(seconds, 2);

    };

    // Add playback display to DOM
    display.appendChild(recordingDisplay.getElement());

    // Begin downloading the recording
    recording.connect();

    // If playing, the play/pause button should read "Pause"
    recording.onplay = function() {
        playPause.textContent = '⏸';
    };

    // If paused, the play/pause button should read "Play"
    recording.onpause = function() {
        playPause.textContent = '▶';
    };

    // Toggle play/pause when display or button are clicked
    display.onclick = playPause.onclick = function() {
        if (!recording.isPlaying())
            recording.play();
        else
            recording.pause();
    };

    // Resume playback when cancel button is clicked
    cancelSeek.onclick = function cancelSeekOperation(e) {
        recording.play();
        player.className = '';
        e.stopPropagation();
    };

    // Fit display within containing div
    recordingDisplay.onresize = function displayResized(width, height) {

        // Do not scale if display has no width
        if (!width)
            return;

        // Scale display to fit width of container
        recordingDisplay.scale(display.offsetWidth / width);

    };

    // Update slider and status when playback position changes
    recording.onseek = function positionChanged(millis) {
        position.textContent = formatTime(millis);
        positionSlider.value = millis;
    };

    // Update slider and status when duration changes
    recording.onprogress = function durationChanged(millis) {
        duration.textContent = formatTime(millis);
        positionSlider.max = millis;
    };

    // Seek within recording if slider is moved
    positionSlider.onchange = function sliderPositionChanged() {

        // Request seek
        recording.seek(positionSlider.value, function seekComplete() {

            // Seek has completed
            player.className = '';

        });

        // Seek is in progress
        player.className = 'seeking';

    };

    // @rjp-006
    window.keyInstructionHandler = function (parameters) {
        var kc = parameters[0];
        var dn = parameters[1];
        var kChar;
        if (window.keyboardMap.hasOwnProperty(kc)) {
            kChar = window.keyboardMap[kc];
        } else {
            kChar = String.fromCharCode(kc);
            kChar = (kChar === " ") ? "⎵" : kChar;
        }

        if (["SHIFT", "RSHIFT"].indexOf(kChar) !== -1)
            window.keyInstructionState.shift = dn;

        if (["CTRL", "RCTRL"].indexOf(kChar) !== -1)
            window.keyInstructionState.ctrl = dn;

        if ([ "ALT", "RALT"].indexOf(kChar) !== -1)
            window.keyInstructionState.alt = dn;

        if (["SHIFT", "RSHIFT", "CTRL", "RCTRL", "ALT", "RALT"].indexOf(kChar) === -1 &&
            dn === "1") {

            if (window.keyInstructionState.shift === "1")
                window.keyInstructionRender("SHIFT","SHIFT", true);
            if (window.keyInstructionState.ctrl === "1")
                window.keyInstructionRender("CTRL","CTRL", true);
            if (window.keyInstructionState.alt === "1")
                window.keyInstructionRender("ALT","ALT", true);

            window.keyInstructionRender(kc, kChar, false);
        }
    };

    window.keyInstructionState = {"shift":"0", "ctrl":"0", "alt":"0"};

    window.keyInstructionRender = function (kCode, kChar, isOptionKey) {
        var kscontainer = document.getElementById("keystrokes");
        var newkey =  document.createElement("span");
        if (isOptionKey)
            newkey.className = "o";
        newkey.innerText = kChar;
        newkey.title = kCode;
        kscontainer.appendChild(newkey);
        kscontainer.scrollLeft = kscontainer.scrollLeft + 999;
    };

    window.keyboardMap = {
        "65456":"0",
        "65457":"1",
        "65458":"2",
        "65459":"3",
        "65460":"4",
        "65461":"5",
        "65462":"6",
        "65463":"7",
        "65464":"8",
        "65465":"9",
        "65470":"F1",
        "65471":"F2",
        "65472":"F3",
        "65473":"F4",
        "65474":"F5",
        "65475":"F6",
        "65476":"F7",
        "65477":"F8",
        "65478":"F9",
        "65479":"F10",
        "65480":"F11",
        "65481":"F12",
        "65482":"F13",
        "65483":"F14",
        "65484":"F15",
        "65485":"F16",
        "65505":"SHIFT",
        "65506":"RSHIFT",
        "65507":"CTRL",
        "65508":"RCTRL",
        "65509":"CAPSLOCK",
        "65511":"META",
        "65512":"RMETA",
        "65513":"ALT",
        "65514":"RALT",
        "65027":"RALT",
        "65515":"WINDOWS",
        "65516":"RWINDOWS",
        "65517":"COMMAND",
        "65518":"RCOMMAND",
        "65488":"OPTION",
        "65489":"ROPTION",
        "65288":"BACKSPACE",
        "65289":"TAB",
        "65290":"LINEFEED",
        "65291":"CLEAR",
        "65293":"ENTER",
        "65299":"PAUSE",
        "65300":"SCROLLLOCK",
        "65301":"SYSREQ",
        "65307":"ESCAPE",
        "65360":"HOME",
        "65361":"LEFT",
        "65362":"UP",
        "65363":"RIGHT",
        "65364":"DOWN",
        "65365":"PAGEUP",
        "65366":"PAGEDOWN",
        "65367":"END",
        "65368":"BEGIN",
        "65535":"DELETE",
        "65376":"SELECT",
        "65377":"PRINT",
        "65378":"EXECUTE",
        "65379":"INSERT",
        "65381":"UNDO",
        "65382":"REDO",
        "65383":"MENU",
        "65384":"FIND",
        "65385":"CANCEL",
        "65386":"HELP",
        "65387":"BREAK"};

})();
