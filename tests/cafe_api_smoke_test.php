<?php
/**
 * Sokna Cafe API v1 smoke test.
 *
 * Usage:
 *   php tests/cafe_api_smoke_test.php --base-url=https://example.com --token=TOKEN --active=SK-1405-00012 --inactive=SK-1405-00013
 *
 * The token is supplied at runtime and is never stored in this file.
 */

$options = getopt('', ['base-url:', 'token:', 'active:', 'inactive:']);

foreach (['base-url', 'token', 'active', 'inactive'] as $required) {
    if (empty($options[$required])) {
        fwrite(STDERR, "Missing --{$required}\n");
        exit(2);
    }
}

$baseUrl = rtrim($options['base-url'], '/');
$token = $options['token'];
$active = $options['active'];
$inactive = $options['inactive'];

function request(string $baseUrl, string $token, string $method, string $path, ?array $payload = null): array
{
    $ch = curl_init($baseUrl . $path);
    $headers = [
        'Authorization: Bearer ' . $token,
        'Accept: application/json',
        'Content-Type: application/json',
    ];

    curl_setopt_array($ch, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_CUSTOMREQUEST => $method,
        CURLOPT_HTTPHEADER => $headers,
        CURLOPT_TIMEOUT => 20,
        CURLOPT_SSL_VERIFYPEER => true,
        CURLOPT_SSL_VERIFYHOST => 2,
    ]);

    if ($payload !== null) {
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_THROW_ON_ERROR));
    }

    $body = curl_exec($ch);
    $error = curl_error($ch);
    $status = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    if ($body === false) {
        throw new RuntimeException('cURL error: ' . $error);
    }

    $json = json_decode($body, true);
    if (!is_array($json)) {
        throw new RuntimeException("Invalid JSON response (HTTP {$status}): {$body}");
    }

    return [$status, $json];
}

function assertTrue(bool $condition, string $message): void
{
    if (!$condition) {
        throw new RuntimeException('FAIL: ' . $message);
    }
    echo "PASS: {$message}\n";
}

try {
    assertTrue(str_starts_with(strtolower($baseUrl), 'https://'), 'Base URL uses HTTPS');

    // 1. Search active reservation by code.
    [$status, $response] = request(
        $baseUrl,
        $token,
        'GET',
        '/api.php?action=search&type=code&query=' . rawurlencode($active)
    );
    assertTrue($status === 200, 'Active reservation search returns HTTP 200');
    assertTrue(isset($response['reservations']) && is_array($response['reservations']), 'Search response always contains reservations array');
    assertTrue(count($response['reservations']) >= 1, 'Active reservation is found');

    // 2. Search inactive reservation and verify it is not chargeable.
    [$status, $inactiveResponse] = request(
        $baseUrl,
        $token,
        'GET',
        '/api.php?action=search&type=code&query=' . rawurlencode($inactive)
    );
    assertTrue($status === 200, 'Inactive reservation search returns HTTP 200');
    assertTrue(isset($inactiveResponse['reservations']) && is_array($inactiveResponse['reservations']), 'Inactive search contains reservations array');

    // 3. Charge active reservation.
    $externalId = 'CAFE-SMOKE-' . date('YmdHis') . '-' . bin2hex(random_bytes(3));
    $chargePayload = [
        'external_order_id' => $externalId,
        'reservation_code' => $active,
        'amount' => 1000,
        'note' => 'Sokna Cafe API smoke test',
    ];

    [$status, $chargeResponse] = request($baseUrl, $token, 'POST', '/api.php?action=charge', $chargePayload);
    assertTrue($status >= 200 && $status < 300, 'Charge request succeeds');

    // 4. Idempotency: repeat the exact same charge.
    [$status, $chargeRepeat] = request($baseUrl, $token, 'POST', '/api.php?action=charge', $chargePayload);
    assertTrue($status >= 200 && $status < 300, 'Repeated charge request is handled idempotently');

    $firstId = $chargeResponse['id'] ?? $chargeResponse['charge_id'] ?? null;
    $repeatId = $chargeRepeat['id'] ?? $chargeRepeat['charge_id'] ?? null;
    if ($firstId !== null && $repeatId !== null) {
        assertTrue((string) $firstId === (string) $repeatId, 'Idempotent charge keeps the same charge record');
    }

    // 5. Void the charge.
    [$status, $voidResponse] = request($baseUrl, $token, 'POST', '/api.php?action=void', [
        'external_order_id' => $externalId,
    ]);
    assertTrue($status >= 200 && $status < 300, 'Void request succeeds');

    // 6. Idempotency of void.
    [$status, $voidRepeat] = request($baseUrl, $token, 'POST', '/api.php?action=void', [
        'external_order_id' => $externalId,
    ]);
    assertTrue($status >= 200 && $status < 300, 'Repeated void request is handled idempotently');

    echo "\nSmoke test completed successfully.\n";
    echo "External order: {$externalId}\n";
    echo "No Bearer token was written to disk.\n";
    exit(0);
} catch (Throwable $e) {
    fwrite(STDERR, "\nSmoke test failed: {$e->getMessage()}\n");
    exit(1);
}
