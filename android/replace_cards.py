import re

# 1. Replace ConnectionControlsCard in HomeScreen.kt
with open('app/src/main/java/com/airmouse/presentation/ui/home/HomeScreen.kt', 'r') as f:
    content = f.read()

# Find the start of ConnectionControlsCard
start_idx = content.find('@Composable\nfun ConnectionControlsCard(')

# Find the end of the function (counting braces)
if start_idx != -1:
    brace_count = 0
    in_function = False
    end_idx = -1
    for i in range(start_idx, len(content)):
        if content[i] == '{':
            brace_count += 1
            in_function = True
        elif content[i] == '}':
            brace_count -= 1
        
        if in_function and brace_count == 0:
            end_idx = i + 1
            break
    
    if end_idx != -1:
        new_card = """@Composable
fun ConnectionControlsCard(
    ip: String,
    port: Int,
    protocol: ConnectionProtocol,
    onIpChange: (String) -> Unit,
    onPortChange: (Int) -> Unit,
    onProtocolChange: (ConnectionProtocol) -> Unit,
    onConnect: () -> Unit,
    onScanQr: () -> Unit,
    isConnecting: Boolean,
    isConnected: Boolean = false,
    isRegistered: Boolean,
    lastServer: String
) {
    val focusManager = LocalFocusManager.current
    
    val infiniteTransition = rememberInfiniteTransition(label = "cardGradient")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )

    Card(
        modifier = Modifier.fillMaxWidth().graphicsLayer {
            shadowElevation = 16.dp.toPx()
            shape = RoundedCornerShape(28.dp)
            clip = true
        },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.surface
                        ),
                        start = androidx.compose.ui.geometry.Offset(gradientOffset, 0f),
                        end = androidx.compose.ui.geometry.Offset(gradientOffset + 500f, 1000f)
                    )
                )
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (protocol == ConnectionProtocol.BLUETOOTH) "Bluetooth Mouse" else "Connect Server",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (lastServer.isNotEmpty()) {
                            Text(
                                text = "Last used: $lastServer", 
                                style = MaterialTheme.typography.labelMedium, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (protocol == ConnectionProtocol.BLUETOOTH) Icons.Default.Bluetooth else Icons.Default.Computer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (!isRegistered) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Waiting for profile registration", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                OutlinedTextField(
                    value = ip,
                    onValueChange = onIpChange,
                    label = { Text(if (protocol == ConnectionProtocol.BLUETOOTH) "MAC Address" else "IP Address") },
                    placeholder = { Text(if (protocol == ConnectionProtocol.BLUETOOTH) "00:11:22:33:44:55 (optional)" else "192.168.1.100") },
                    leadingIcon = { 
                        Icon(
                            if (protocol == ConnectionProtocol.BLUETOOTH) Icons.Default.Bluetooth else Icons.Default.Wifi, 
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isConnecting && !isConnected,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (protocol == ConnectionProtocol.BLUETOOTH || protocol == ConnectionProtocol.USB_SERIAL) ImeAction.Done else ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (!isConnecting && !isConnected) onConnect()
                        }
                    )
                )

                AnimatedVisibility(
                    visible = protocol != ConnectionProtocol.BLUETOOTH && protocol != ConnectionProtocol.USB_SERIAL,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    OutlinedTextField(
                        value = port.toString(),
                        onValueChange = { it.toIntOrNull()?.let(onPortChange) },
                        label = { Text("Port") },
                        placeholder = { Text("8082") },
                        leadingIcon = { 
                            Icon(
                                Icons.Default.SettingsEthernet, 
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            ) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (!isConnecting && !isConnected) onConnect()
                            }
                        ),
                        enabled = !isConnecting && !isConnected,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Protocol",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            ConnectionProtocol.UDP to "UDP",
                            ConnectionProtocol.WEBSOCKET to "WebSocket",
                            ConnectionProtocol.TCP to "TCP",
                            ConnectionProtocol.USB_SERIAL to "USB"
                        ).forEach { (prot, label) ->
                            val isSelected = protocol == prot
                            Surface(
                                onClick = { if (!isConnected) onProtocolChange(prot) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier.animateContentSize()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                    }
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = when (protocol) {
                            ConnectionProtocol.UDP -> "Ultra-low latency for direct local control."
                            ConnectionProtocol.WEBSOCKET -> "Reliable control with web-based servers."
                            ConnectionProtocol.TCP -> "Direct socket connection for custom setups."
                            ConnectionProtocol.USB_SERIAL -> "Zero-latency wired connection."
                            ConnectionProtocol.BLUETOOTH -> "Native wireless mouse emulation."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onConnect,
                    enabled = !isConnecting && isRegistered && !isConnected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    if (isConnecting) {
                        SweepGradientGlowRing(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                            glowRadius = 4.dp,
                            colors = listOf(
                                MaterialTheme.colorScheme.onPrimary,
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Connecting...", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    } else if (isConnected) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connected", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    } else {
                        Icon(Icons.Default.RocketLaunch, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(if (protocol == ConnectionProtocol.BLUETOOTH) "Broadcast" else "Launch Connection", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }

                if (protocol != ConnectionProtocol.BLUETOOTH) {
                    OutlinedButton(
                        onClick = onScanQr,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        enabled = !isConnecting && isRegistered && !isConnected
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Quick Scan QR", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}"""
        
        new_content = content[:start_idx] + new_card + content[end_idx:]
        with open('app/src/main/java/com/airmouse/presentation/ui/home/HomeScreen.kt', 'w') as f:
            f.write(new_content)
        print("Updated HomeScreen.kt")

# 2. Replace ManualConnectDialog in NetworkDiscoveryScreen.kt
with open('app/src/main/java/com/airmouse/presentation/ui/network/NetworkDiscoveryScreen.kt', 'r') as f:
    content = f.read()

start_idx = content.find('@Composable\nfun ManualConnectDialog(')
if start_idx != -1:
    brace_count = 0
    in_function = False
    end_idx = -1
    for i in range(start_idx, len(content)):
        if content[i] == '{':
            brace_count += 1
            in_function = True
        elif content[i] == '}':
            brace_count -= 1
        
        if in_function and brace_count == 0:
            end_idx = i + 1
            break
            
    if end_idx != -1:
        new_dialog = """@Composable
fun ManualConnectDialog(
    onDismiss: () -> Unit,
    onConnect: (String, Int) -> Unit,
    initialIp: String,
    initialPort: Int
) {
    var ip by remember { mutableStateOf(initialIp) }
    var port by remember { mutableStateOf(initialPort.toString()) }
    var isValidIp by remember { mutableStateOf(true) }

    val portInt = port.toIntOrNull()
    val isValidPort = portInt != null && portInt in 1..65535

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 24.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.AddLink, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Column {
                        Text(
                            "Manual Connect", 
                            style = MaterialTheme.typography.titleLarge, 
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Direct server IP entry",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = ip,
                    onValueChange = {
                        ip = it
                        isValidIp = it.matches("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$".toRegex()) || it.isEmpty()
                    },
                    label = { Text("IP Address") },
                    placeholder = { Text("192.168.1.100") },
                    leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    isError = !isValidIp && ip.isNotEmpty(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
                if (!isValidIp && ip.isNotEmpty()) {
                    Text(
                        "Please enter a valid IPv4 address",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter { c -> c.isDigit() } },
                    label = { Text("Port") },
                    leadingIcon = { Icon(Icons.Default.SettingsEthernet, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    isError = !isValidPort && port.isNotEmpty(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
                if (!isValidPort && port.isNotEmpty()) {
                    Text(
                        "Port must be between 1 and 65535",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            if (ip.isNotEmpty() && isValidIp && isValidPort && portInt != null) {
                                onConnect(ip, portInt)
                            }
                        },
                        enabled = isValidIp && ip.isNotEmpty() && isValidPort,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}"""
        new_content = content[:start_idx] + new_dialog + content[end_idx:]
        with open('app/src/main/java/com/airmouse/presentation/ui/network/NetworkDiscoveryScreen.kt', 'w') as f:
            f.write(new_content)
        print("Updated NetworkDiscoveryScreen.kt")
